#ifndef _THC_PKT_HDR_H
#define _THC_IPV6_H

#include <inttypes.h>

struct thc_addr6 {
  char addr6[16];
};

struct thc_ipv6_hdr {
  union {
    uint32_t whole;
    struct {
      uint32_t flow_label:20;
      uint32_t traffic_class:8;
      uint32_t version:4;
	} fields;
  } ver_cls_flw;
  
  uint16_t payload_len;
  uint8_t next_hdr;
  uint8_t hop_limit;
  struct thc_addr6 source;
  struct thc_addr6 destination;
  char *data;
};

struct thc_icmpv6_hdr {
  uint8_t type;
  uint8_t code;
  uint16_t checksum;
  char *data; 
};

struct thc_mldv2_query_hdr{
  uint16_t max_resp_code;
  uint16_t reserved;
  struct thc_addr6 multic_addr6;
  
  union {
	uint8_t whole;
	struct {
		uint8_t q_robust_var:3;
		uint8_t s:1;
		uint8_t reserved:4;
	} fields;
  } sflag_qrv;
  
  uint8_t q_q_interval_code;
  uint16_t num_sources;
  struct thc_addr6 source; // Currently supporting only one source
};

struct thc_mldv2_report_hdr {
  uint16_t reserved;
  uint16_t num_multic_addr6_recs;
  struct thc_addr6 *multic_addr6_recs;
};

struct thc_ipv6_option_hdr {
	uint8_t type;
	uint8_t len;
};

struct thc_neigh_adv_hdr {
    union {
	uint32_t whole;
	struct {
		uint32_t r:1;
		uint32_t s:1;
		uint32_t o:1;
        uint32_t reserved:29;
	} fields;
  } flags;
  struct thc_addr6 target_address;
};

#endif
