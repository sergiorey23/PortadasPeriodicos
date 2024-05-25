package sergirex.portadasperiodicos;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Sergio on 12/02/2017.
 * Mierda
 */

public class Deportes extends Fragment {

    public static Deportes newInstance(String fecha) {
        Bundle args = new Bundle();
        args.putString("fecha", fecha);
        Deportes d = new Deportes();
        d.setArguments(args);
        return d;
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.portada_layout, container, false);
        String fecha = getArguments().getString("fecha");
        GetPortadas getPortadas = new GetPortadas(rootView, getClass().getSimpleName(), fecha,null);
        getPortadas.execute(Periodicos.deportes);
        SwipeRefreshLayout swipeRefreshLayout = rootView.findViewById(R.id.refreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshFragment(rootView, swipeRefreshLayout, fecha);
        });
        return getPortadas.getRootView();
    }

    private void refreshFragment(View rootView, SwipeRefreshLayout swipeRefreshLayout, String fecha) {
        ViewGroup viewGroup = rootView.findViewById(R.id.linearLayout);
        viewGroup.removeAllViews();
        GetPortadas getPts = new GetPortadas(rootView, getClass().getSimpleName(),fecha, swipeRefreshLayout);
        getPts.execute(Periodicos.deportes);
    }
}
