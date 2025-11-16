package sergirex.portadasperiodicos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class General extends Fragment implements GetPortadas.PortadasListener {

    private PortadasAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String fecha;

    public static General newInstance(String fecha) {
        Bundle args = new Bundle();
        args.putString("fecha", fecha);
        General g = new General();
        g.setArguments(args);
        return g;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.portada_layout, container, false);

        assert getArguments() != null;
        fecha = getArguments().getString("fecha");

        // Set up RecyclerView
        RecyclerView recyclerView = rootView.findViewById(R.id.recyclerView);
        int spanCount = getResources().getBoolean(R.bool.isTablet) ? 3 : 2;
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
        adapter = new PortadasAdapter(getContext());
        recyclerView.setAdapter(adapter);

        // Set up SwipeRefreshLayout
        swipeRefreshLayout = rootView.findViewById(R.id.refreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this::refreshFragment);

        // Fetch data
        loadPortadas();

        return rootView;
    }

    private void loadPortadas() {
        if (getContext() != null) {
            GetPortadas getPortadas = new GetPortadas(getContext(), getClass().getSimpleName(), fecha, swipeRefreshLayout, this);
            getPortadas.execute(Periodicos.general);
        }
    }

    private void refreshFragment() {
        adapter.clear();
        Calendar calendar = Calendar.getInstance();
        Date today = new Date();
        calendar.setTime(today);
        if (calendar.get(Calendar.HOUR_OF_DAY) < 6) {
            calendar.add(Calendar.DATE, -1);
        }
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd", Locale.FRANCE);
        fecha = formatter.format(calendar.getTime());
        loadPortadas();
    }

    @Override
    public void onPreExecute() {
        if (!swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(true);
        }
    }

    @Override
    public void onPortadaLoaded(GetPortadas.PortadaResult result) {
        if (adapter != null) {
            adapter.addPortada(result);
        }
    }

    @Override
    public void onComplete() {
        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }
}