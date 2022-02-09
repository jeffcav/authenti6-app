/**
 * @file ipv6-agent.c
 * @brief Measures RTT and IAT between local computer and an IPv6-capable devices.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <sys/resource.h>
#include <sys/wait.h>
#include <time.h>
#include <fcntl.h>
#include <pcap.h>
#include <linux/ipv6.h>
#include <sys/stat.h>
#include <dirent.h>
#include <errno.h>
#include "thc-ipv6.h"
#include "thc-headers.h"

#define AUTHOR_NAME "Jefferson Cavalcante"
#define AUTHOR_ORGANIZATION "UFC"

#define DEFAULT_MAX_SAMPLES 10
#define DEFAULT_TIMEOUT_SEC 2
#define DEFAULT_IAT_TIMEOUT_SEC 1

/** @brief Number of responses to the Query to protect against loss. Default is 2, max is 7.
 *
 *  Note: Some devices refuses non-default value, which will cause timeouts in our program.
 */
#define DEFAULT_MLDV2_ROBUSTNESS 2

/** @brief Querier's Query Interval Code, interval between consecutive MLDv2 Reports. Default is 120. */
#define DEFAULT_MLDV2_QQIC 1

/** @brief The maximum time a node can delay the response of a MLDv2 Query. Default is 1092. */
#define DEFAULT_MLDV2_MAX_RESP_CODE 1

#define MAX_IPV6_STRING_LEN 100
#define MAX_FILENAME_LEN 300

/** @brief Seconds to wait before we start measurements */
#define WAIT_STABILIZATION 3

#define MAX_NUM_TIMEOUTS 15

enum mode {
	LEARN_MODE=1,
	MEASURE_MODE=2
};

struct configuration {
	int is_valid;
	char *interface;
	enum mode mode;

	unsigned char destination_ip6[MAX_IPV6_STRING_LEN];

	int max_samples_rtt;
	int max_samples_iat;
	int timeout_sec;
	int timeout_iat_sec;

	int mldv2_robustness;
	int mldv2_qq_interval_code;
	int mldv2_max_resp_code;
};

/* Global variables used while capturing packets */
struct timespec tstart, tend;
struct timespec rtt_begin, rtt_end;
struct timespec report0, report1;
pcap_t *pcap_link;
char destination_ipv6[MAX_IPV6_STRING_LEN];
char destination_ipv6_binary[16];
unsigned char my_ipv6_binary[16];

// pipes for communication with the authentication server
char REQUESTS_FIFO[] = "/tmp/requests-pipe";
char EVENTS_FIFO[] = "/tmp/events-pipe";

/**
 * @brief Parses command-line arguments to detect configuration parameters.
 *
 * This function could be much improved with error handling and etc...
 */
struct configuration make_config(int argc, char *argv[]) {
	struct configuration config;

	if (argc < 2) {
		config.is_valid = 0;
		return config;
	}

	config.is_valid = 1;
	config.mode = MEASURE_MODE;

	config.timeout_sec = DEFAULT_TIMEOUT_SEC;
	config.timeout_iat_sec = DEFAULT_IAT_TIMEOUT_SEC;
	config.max_samples_rtt = DEFAULT_MAX_SAMPLES;
	config.max_samples_iat = DEFAULT_MAX_SAMPLES;

	config.mldv2_robustness = DEFAULT_MLDV2_ROBUSTNESS;
	config.mldv2_qq_interval_code = DEFAULT_MLDV2_QQIC;
	config.mldv2_max_resp_code = DEFAULT_MLDV2_MAX_RESP_CODE;

	//TODO: remove next line
	//config.destination_ip6 = (unsigned char*) destination_ipv6;

	switch(argc) {
		case 5: // optional
			config.mode = atoi(argv[4]);
		case 4: // optional
			config.max_samples_iat = atoi(argv[3]);
		case 3: // optional
			config.max_samples_rtt = atoi(argv[2]);
		case 2: // mandatory
			config.interface = argv[1];
			break;
	}

	printf("\n\n");
	printf("######################################\n");
	printf("Interface = %s\n", config.interface);
	printf("Mode = %s\n", config.mode == LEARN_MODE? "Learning":"Measuring");
	printf("Max. RTT samples = %d\n", config.max_samples_rtt);
	printf("Max. IAT samples = %d\n", config.max_samples_iat);
	printf("MAx. consecutive timeouts: %d\n", MAX_NUM_TIMEOUTS);
	printf("IGNORING PROFILED DEVICES.\n");
	printf("######################################\n\n");

	return config;
}

void help(char *prg) {
	printf("%s %s (c) 2019 by %s - %s\n\n", prg, "1.0", AUTHOR_NAME, AUTHOR_ORGANIZATION);
	printf("Syntax: %s interface IPv6_destination [num_solicitations]\n\n", prg);
	exit(-1);
}

/**
 * @brief Utility function which returns the difference between timespecs in seconds.
 *
 * @return Time in seconds in floating point format.
 */
double my_diff_time(struct timespec begin, struct timespec end) {
	return (((double)end.tv_sec + 1.0e-9*(double)end.tv_nsec) -
	((double)begin.tv_sec + 1.0e-9*(double)begin.tv_nsec));
}

/**
 * @brief Process each IPv6 packet which flows through the configured interface.
 *
 * We listen to all packets that flows a given interface until a Neighbor Advertisement
 * arrives or a timeout occurs.
 */
void handle_neigh_advs(u_char* foo, const struct pcap_pkthdr *header, const unsigned char *data) {
	unsigned char *ipv6hdr = (unsigned char *) (data + 14);
	struct thc_ipv6_hdr *ip6_hdr = (struct thc_ipv6_hdr *) (data+14);
	struct thc_icmpv6_hdr *icmp6_hdr = (struct thc_icmpv6_hdr *) (data+62);
	struct thc_neigh_adv_hdr *nadv_hdr;

	if (ipv6hdr[6] != NXT_ICMP6 || ipv6hdr[40] != ICMP6_NEIGHBORADV)
		return;

	// Check destination address
	if (memcmp((char*)&ip6_hdr->destination, my_ipv6_binary, 16) != 0)
		return;

	// Check source address
	if (memcmp((char*)&ip6_hdr->source, destination_ipv6_binary, 16) != 0)
		return;

	// Check if the s (solicited) flag is set
	nadv_hdr = (struct thc_neigh_adv_hdr*) &icmp6_hdr->data;
	if (nadv_hdr->flags.fields.s == 0)
		return;

	// Fetching arrival time as soon as possible
	clock_gettime(CLOCK_MONOTONIC, &tend);

	// Disabling timeout alarm
	alarm(0);

	pcap_breakloop(pcap_link);
	return;
}

/**
 * @brief Stops a pcap listener currently running.
 */
void alarm_handler(int sig)
{
	pcap_breakloop(pcap_link);
}

/**
 * @brief Setup a live pcap listener to process IPv6 packets
 */
int prepare_pcap(char *interface, unsigned char *capture) {
	pcap_link = NULL;
	char errbuf[PCAP_ERRBUF_SIZE];
	struct bpf_program fcode;
	char default_interface[] = "eth0";

	if (interface == NULL)
		interface = default_interface;
	if ((pcap_link = pcap_open_live(interface, 65535, 0, -1, errbuf)) == NULL) //timeout=3s
		return -1;
	if (pcap_compile(pcap_link, &fcode, capture, 1, 0) < 0)
		return -2;
	pcap_setfilter(pcap_link, &fcode);
}

/**
 * @brief Iterate over packets flowing through a given interface
 */
int wait_neighbor_advertisement() {
	alarm(DEFAULT_TIMEOUT_SEC);

	if (pcap_loop(pcap_link, -1, (pcap_handler) handle_neigh_advs, NULL) < 0)
		return -1;
	return 0;
}

/**
 * @brief Stores measurements in a file.
 *
 * Filename has format: data/device_ipv6/measure.txt, where measure could be rtt or iat.
 */
void save_measurements(struct configuration *config, char *measure, double *data, int num_data) {
	DIR *dir;
	FILE *fp;
	int status, len, i;
	char filename[MAX_FILENAME_LEN] = {'\0'};
	char folder[MAX_FILENAME_LEN] = {'\0'};

	// Create directory to store data for this device if needed
	snprintf(folder, MAX_FILENAME_LEN, "%s/%s", "data", config->destination_ip6);

	dir = opendir(folder);
	if (!dir) {
		status = mkdir("data", S_IRWXU | S_IRWXG | S_IROTH | S_IXOTH);
		snprintf(folder, MAX_FILENAME_LEN, "%s/%s", "data", (char *) config->destination_ip6);
		status = mkdir(folder, S_IRWXU | S_IRWXG | S_IROTH | S_IXOTH);
	}

	// Setup filename
	snprintf(filename, MAX_FILENAME_LEN, "%s/%s.txt", folder, measure);

	// Write measurements in the file
	printf("Saving measurements to: %s...\n", filename);

	fp = fopen(filename, "w+");
	if (!fp) {
		printf("Error opening %s.\n", filename);
		return;
	}

	for (i = 0; i < num_data; i++) {
		fprintf(fp, "%.9lf\n", data[i]); // Nanossecond precision
	}

	fclose(fp);
}


int send_neighbor_solicits(struct configuration *config, unsigned char *ip6, unsigned char *mac6, unsigned char *dst, unsigned char *dstmac) {
	double rtt;
	int consecutive_timeouts;
	int loss = 0, num_rtts = 0, err = 0;
	double rtts[config->max_samples_rtt];
	unsigned char *pkt;
	int pktlen;
	char timeout;

	pktlen = 0;
	pkt = thc_generate_neighborsol6(config->interface, ip6, dst, NULL, mac6, dstmac, &pktlen);

	// Discard first RTT (possibly not reliable due to physical address translation, etc)
	thc_send_pkt(config->interface, pkt, &pktlen);
	wait_neighbor_advertisement();

	while (num_rtts < config->max_samples_rtt && consecutive_timeouts < MAX_NUM_TIMEOUTS) {
		tstart = tend = (struct timespec){0, 0};
		clock_gettime(CLOCK_MONOTONIC, &tstart);

		thc_send_pkt(config->interface, pkt, &pktlen);
		wait_neighbor_advertisement();

		// If we got no answer
		if (tend.tv_sec == 0 && tend.tv_nsec == 0) {
			loss++;
			consecutive_timeouts++;
			printf("Loss. %d RTTs remaining.\n", config->max_samples_rtt - num_rtts);
			continue;
		}

		// A wild RTT appeared
		consecutive_timeouts = 0;
		rtt = my_diff_time(tstart, tend);

		// Avoid very small RTT (less than 100us) caused by duplicated neighbor adv
		if (rtt <= 0.0001) {
			printf("Duplicated Neighbor Advertisement. Cooling down a bit\n");
			sleep(1); // Sleep 1s before proceeding
			continue;
		}

		// In case of successful RTT
		rtts[num_rtts++] = rtt;
	}

	printf("\nRTTs: %d LOSS: %d\n", num_rtts, loss);

	if (consecutive_timeouts >= MAX_NUM_TIMEOUTS) {
		printf("Device not responding. ABORTED\n");
		return 1;
	}

	save_measurements(config, "rtt", rtts, num_rtts);

	return 0;
}

/**
 * @brief Creates a Multicast Solicited-Node address from an unicast address;
 */
void make_multic_solicit_node(unsigned char *unicast) {
	int byte;
	char *multic = thc_resolve6("FF02::1:FF00:0000");

	for (byte = 0; byte < 13; byte++) {
		unicast[byte] = multic[byte];
	}

	free(multic);
}

/**
 * @brief Builds a MLDv2 Query packet according to RFC 3810.
 *
 * Address-Specific Query configuration:
 *   IPv6 destination = FF02::1 (all nodes)
 *   Query Multicast Address = Multicast Solicited-Node BIO_get_conn_address
 *   Query Source[0] = IPv6 of destination
 *
 * @param config Contains interface, source and destination IPv6 and multicast address.
 */
char* make_mldv2_packet(struct configuration *config) {
	char *pkt;
	char *src6, *dst6, *unicast_dst6, mcast_solicit[16];
	int pkt_len, ret, icmpv6_flags;
	unsigned char *src_mac, *dst_mac;
	struct thc_mldv2_query_hdr query;
	struct thc_ipv6_option_hdr ipv6_options[3];

	// Convert addresses from text to network format
	src6 = thc_resolve6(thc_get_own_ipv6(config->interface, NULL, PREFER_LINK));
	dst6 = thc_resolve6("ff02::1"); // All nodes ff02::1
	unicast_dst6 = thc_resolve6(config->destination_ip6);
	memcpy(mcast_solicit, unicast_dst6, 16);
	make_multic_solicit_node((unsigned char *) mcast_solicit);

	// MAC addresses
	src_mac = thc_get_own_mac(config->interface);
	dst_mac = NULL;

	// Configure fields of the query
	memset(&query, 0, sizeof(struct thc_mldv2_query_hdr));
	query.max_resp_code = config->mldv2_max_resp_code;
	query.sflag_qrv.fields.q_robust_var = config->mldv2_robustness;
	query.sflag_qrv.fields.s = 0;
	query.q_q_interval_code = config->mldv2_qq_interval_code;
	query.num_sources = ((uint16_t)1<<8);
	memcpy(&query.multic_addr6, mcast_solicit, 16);
	memcpy(&query.source, unicast_dst6, 16);

	// Crete packet structure representation
	pkt_len = 0;
	if ((pkt = thc_create_ipv6(config->interface, PREFER_LINK, &pkt_len,
							(unsigned char*) src6, (unsigned char *) dst6,
							1, 0, 0, 0, 0)) == NULL) {
		printf("Could not instantiate packet.\n");
		return NULL;
	}

	// Add Hop-by-Hop header Options header with a Router Alert, followed
	// by two paddings. This is mandatory by RFC 3810, section 5.
	memset(ipv6_options, 0, sizeof(ipv6_options));
	ipv6_options[0] = (struct thc_ipv6_option_hdr) {
		.type=5, //Router Alert = 0x5
		.len=2
	};

	if (thc_add_hdr_hopbyhop((unsigned char*) pkt, &pkt_len,
							(unsigned char*) ipv6_options, 6) < 0) {
		printf("Could not add Hop by Hop header into packet.\n");
		thc_destroy_packet((unsigned char*)pkt);
		return NULL;
	}

	// Add MLDv2 Query as an ICMPv6 packet.
	icmpv6_flags = (query.max_resp_code << 16) | query.reserved;
	if (thc_add_icmp6((unsigned char *) pkt, &pkt_len, ICMP6_MLD_QUERY,
					0, icmpv6_flags, (unsigned char *) &query.multic_addr6,
					36, 0) < 0) { //size=20 without source
		printf("Could not add MLDv2 Query data into packet.\n");
		thc_destroy_packet((unsigned char*)pkt);
		return NULL;
	}

	// Finish packet structure inserting link-layer header
	ret = thc_generate_pkt(config->interface, src_mac, dst_mac, (unsigned char *) pkt, &pkt_len);
	if (ret) {
		printf("Could not generate packet.\n");
		thc_destroy_packet((unsigned char*)pkt);
		return NULL;
	}

	return pkt;
}

/**
 * @brief Process each IPv6 packet and check if it is a MLDv2 Report.
 *
 * We listen to all packets that flows a given interface until the expected
 * MLDv2 Report arrives or a timeout occurs.
 *
 * TODO: Address case in which Hop-by-Hop Options header isn't present (possible?).
 * TODO: Check if multicast address matches the expected.
 */
void handle_mldv2_reports(u_char* foo, const struct pcap_pkthdr *header, const unsigned char *data) {
	unsigned char *src;

	struct thc_ipv6_hdr *ip6_hdr = (struct thc_ipv6_hdr *) (data+14);
	struct thc_icmpv6_hdr *icmp6_hdr = (struct thc_icmpv6_hdr *) (data+62);

	clock_gettime(CLOCK_MONOTONIC, &tend);

	if (ip6_hdr->next_hdr == NXT_HOP && icmp6_hdr->type == ICMP6_MLD2_REPORT) {
		src = thc_ipv62notation((unsigned char*) &ip6_hdr->source);

		if (strcmp((char*)src, destination_ipv6) == 0) {
			alarm(0); //Disabling timeout alarm
			pcap_breakloop(pcap_link);
			return;
		}
	}

	tend = (struct timespec){0, 0};
}

/**
 * @brief Iterate over packets flowing through a given interface
 */
int wait_for_mldv2_report() {
	alarm(DEFAULT_IAT_TIMEOUT_SEC);

	if (pcap_loop(pcap_link, -1, (pcap_handler) handle_mldv2_reports, NULL) < 0)
		return -1;
	return 0;
}

int send_mldv2_queries(struct configuration *config) {
	char *pkt;
	int err, i, pkt_len = 0, iat_idx = 0, timeout = 0;
	int iats_remaining = config->max_samples_iat;
	int num_reports = config->mldv2_robustness;
	int num_timeouts = 0, consecutive_timeouts = 0;
	double iats[iats_remaining];

	pkt = make_mldv2_packet(config);

	while (iats_remaining && consecutive_timeouts < MAX_NUM_TIMEOUTS) {
		num_reports = config->mldv2_robustness;
		tstart = tend = (struct timespec){0, 0};

		thc_send_pkt(config->interface, (unsigned char*) pkt, &pkt_len);

		while (num_reports--) {
			wait_for_mldv2_report();

			// Timeout
			if (tend.tv_sec == 0 || tend.tv_nsec == 0) {
				printf("Loss. %d measurements remaining\n", iats_remaining);
				timeout = 1;
				num_timeouts++;
				consecutive_timeouts++;
				break;
			}

			// New IAT measurement
			if (tstart.tv_sec != 0 || tstart.tv_nsec != 0) {
				if (iats_remaining) {
					iats[iat_idx++] = my_diff_time(tstart, tend);
					iats_remaining--;
				}
			}

			consecutive_timeouts = 0;
			tstart = tend;
		}
	}

	printf("\nIATs: %d LOSS: %d\n", config->max_samples_iat - iats_remaining, num_timeouts);

	if (consecutive_timeouts >= MAX_NUM_TIMEOUTS) {
		printf("Device not responding. ABORTED\n");
		return 1;
	}

	save_measurements(config, "iat", iats, config->max_samples_iat);

	return 0;
}

int is_device_known(char *device_ipv6) {
	DIR *dir;
	char folder[MAX_FILENAME_LEN] = {'\0'};

	// Create directory store data for this device
	snprintf(folder, MAX_FILENAME_LEN, "%s/%s", "data", device_ipv6);

	dir = opendir(folder);
	if (dir) {
		return 1;
	}

	return 0;
}

// Handles Duplicate Address Detection packets
void handle_dad(u_char *foo, const struct pcap_pkthdr *header, const unsigned char *data) {
	unsigned char *target;

	// :: (all zeros)
	const char unspecified_addr[16] = {
		0x0, 0x0, 0x0, 0x0,
		0x0, 0x0, 0x0, 0x0,
		0x0, 0x0, 0x0, 0x0,
		0x0, 0x0, 0x0, 0x0
	};

	// ff02::1:ffXX:XXXX
	const char solicited_node[13] = {
		0xff, 0x02, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x01,
		0xff
	};

	struct thc_ipv6_hdr *ip6_hdr = (struct thc_ipv6_hdr *) (data+14);
	struct thc_icmpv6_hdr *icmp6_hdr = (struct thc_icmpv6_hdr *) (data+54);

	// Is it a Neighbor Solicitation?
	if (ip6_hdr->next_hdr != NXT_ICMP6 || icmp6_hdr->type != ICMP6_NEIGHBORSOL)
		return;

	// Is the source ipv6 the unspecified address?
	if (memcmp((char*) &ip6_hdr->source, unspecified_addr, 16) != 0)
		return;

	// Is the destination ipv6 a solicited-node multicast address?
	if (memcmp((char*) &ip6_hdr->destination, solicited_node, 13) != 0)
		return;
	
	// A wild device appeared!
	target = thc_ipv62notation((unsigned char*) &icmp6_hdr->data);
	if (is_device_known((char *) target))
		return;

	// Gotcha!
	alarm(0);
	pcap_breakloop(pcap_link);

	strncpy(destination_ipv6, (char *) target, MAX_IPV6_STRING_LEN);
	memcpy(destination_ipv6_binary, (char*) &icmp6_hdr->data, 16);
	printf("New device detected: %s.\n", target);

	free(target);
	return;
}

int wait_for_dup_addr_detect() {
	if (pcap_loop(pcap_link, -1, (pcap_handler) handle_dad, NULL) < 0)
		return -1;
	return 0;
}

void tell_auth_server(char *event_type, char *device_ip6, unsigned char *device_mac) {
	int fd;
	char event[256];
	char mac_str[20];

	memset(mac_str, '\0', 20);
	
	snprintf(mac_str, 20, "%02x:%02x:%02x:%02x:%02x:%02x", device_mac[0], device_mac[1], 
			device_mac[2], device_mac[3], device_mac[4], device_mac[5]);
	
	snprintf(event, 256, "{ \"event-type\": \"%s\", \"ipv6-address\": \"%s\", \"mac-address\": \"%s\" }\n", event_type, device_ip6, mac_str);
	
	fd = open(EVENTS_FIFO, O_WRONLY);
	write(fd, event, strlen(event));
	close(fd);
}

void wait_for_request() {
	int fd;
	char request[60];

	fd = open(REQUESTS_FIFO, O_RDONLY);
	read(fd, request, 500);
	close(fd);
}

int main(int argc, char* argv[]) {
	char mac[6];
	char *dst;
	unsigned char *ip6;
	unsigned char *dstmac;
	unsigned char *mac6 = mac;
	struct timespec begin, end;

	double rtt;
	int loss = 0;
	int num_rtts = 0;

	int ret;
	struct configuration config;

	config = make_config(argc, argv);

	if (!config.is_valid)
		help(argv[0]);

	signal(SIGALRM, alarm_handler);
	if (prepare_pcap(config.interface, "ip6")) {
		printf("Invalid interface. ABORTED.\n");
		exit(1);
	}

	mac6 = thc_get_own_mac(config.interface);
	ip6 = thc_get_own_ipv6(config.interface, NULL, PREFER_LINK);
	memcpy(my_ipv6_binary, ip6, 16);

	while (1) {
		printf("Waiting while a device enters the network...\n");
		wait_for_dup_addr_detect();

		strncpy((char*) config.destination_ip6, destination_ipv6, MAX_IPV6_STRING_LEN);
		printf("\n================ %s =====================\n", config.destination_ip6);

		printf("Waiting %d seconds for protocols stabilization...\n\n", WAIT_STABILIZATION);
		sleep(WAIT_STABILIZATION);

		clock_gettime(CLOCK_MONOTONIC, &begin);

		dst = thc_resolve6((unsigned char *)config.destination_ip6);
		dstmac = thc_get_mac(config.interface, ip6, (unsigned char *) dst);

		if (!dst || !dstmac) {
			printf("Could not determine MAC address of the device. ABORTED!\n\n");
			continue;
		}

		tell_auth_server("NEW_DEVICE", destination_ipv6, dstmac);
		wait_for_request();
		tell_auth_server("MEASURED_DEVICE", destination_ipv6, dstmac);

		printf("Ready\n");

		// TODO remove
		continue;

		printf("Computing %d RTTs...\n", config.max_samples_rtt);
		if (send_neighbor_solicits(&config, ip6, mac6, dst, dstmac))
			continue;

		printf("\nComputing %d IATs...\n", config.max_samples_iat);
		if (send_mldv2_queries(&config))
			continue;

		alarm(0);

		clock_gettime(CLOCK_MONOTONIC, &end);
		printf("Duration: %lfs\n", my_diff_time(begin, end));

		printf("Done.\n");
		printf("======================================================================\n\n");

		free(dst);
		free(dstmac);
	}

	free(ip6);
	free(mac6);
	return 0;
}
