package sergirex.portadasperiodicos;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PortadasAdapter extends RecyclerView.Adapter<PortadasAdapter.PortadaViewHolder> {

    private final List<GetPortadas.PortadaResult> portadas = new ArrayList<>();
    private final Context context;

    public PortadasAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public PortadaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.portada_item, parent, false);
        return new PortadaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PortadaViewHolder holder, int position) {
        GetPortadas.PortadaResult result = portadas.get(position);
        holder.portadaImageView.setImageBitmap(result.bitmap());

        // Set the click listener to open the detail view
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PortadaDetalle.class);
            intent.putExtra("portada", result.portada());
            intent.putExtra("fecha", result.originalFecha());
            intent.putExtra("allportadas", result.allPortadas());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return portadas.size();
    }

    public void addPortada(GetPortadas.PortadaResult portada) {
        portadas.add(portada);
        notifyItemInserted(portadas.size() - 1);
    }

    public void clear() {
        int size = portadas.size();
        portadas.clear();
        notifyItemRangeRemoved(0, size);
    }

    static class PortadaViewHolder extends RecyclerView.ViewHolder {
        ImageView portadaImageView;

        public PortadaViewHolder(@NonNull View itemView) {
            super(itemView);
            portadaImageView = itemView.findViewById(R.id.portadaImageView);
        }
    }
}