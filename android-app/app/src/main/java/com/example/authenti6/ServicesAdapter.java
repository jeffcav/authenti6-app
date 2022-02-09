package com.example.authenti6;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.w3c.dom.Text;

import java.util.List;

public class ServicesAdapter extends RecyclerView.Adapter<ServicesAdapter.ViewHolder> {
    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView nameView;
        public Button goButton;

        public ViewHolder(View itemView) {
            super(itemView);

            nameView = (TextView) itemView.findViewById(R.id.service_name);
            goButton = (Button) itemView.findViewById(R.id.go_button);
        }
    }

    private List<Service> servicesList;
    private Context context;

    public ServicesAdapter(List<Service> services, Context context) {
        servicesList = services;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        View serviceView = inflater.inflate(R.layout.service_item, parent, false);

        return new ViewHolder(serviceView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Service service = servicesList.get(position);

        TextView textView = holder.nameView;
        textView.setText(service.getName());

        Button button = holder.goButton;
        button.setText("Go");
        button.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(service.getUrl()));
            context.startActivity(browserIntent);
        });

        button.setEnabled(true);
    }

    @Override
    public int getItemCount() {
        return servicesList.size();
    }
}
