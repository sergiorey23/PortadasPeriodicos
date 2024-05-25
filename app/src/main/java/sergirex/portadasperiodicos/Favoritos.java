package sergirex.portadasperiodicos;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * Created by sergio on 01/10/2019.
 * Nothing else to add
 */
public class Favoritos extends Fragment {

    public static Favoritos newInstance(String fecha) {
        Bundle args = new Bundle();
        args.putString("fecha", fecha);
        Favoritos f = new Favoritos();
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.portada_layout, container, false);
        SharedPreferences prefs = rootView.getContext().getSharedPreferences("periodicos", Context.MODE_PRIVATE);
        if(prefs.getAll().isEmpty())
            return rootView;
        String fecha = getArguments().getString("fecha");
        GetPortadas getPortadas = new GetPortadas(rootView, getClass().getSimpleName(),fecha, null);
        String[] favPeriodicos = prefs.getAll().values().toArray(new String[0]);
        getPortadas.execute(favPeriodicos);
        SwipeRefreshLayout swipeRefreshLayout = rootView.findViewById(R.id.refreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshFragment(rootView, swipeRefreshLayout, prefs, fecha);
            swipeRefreshLayout.setRefreshing(false);
        });
        return getPortadas.getRootView();
    }

    private void refreshFragment(View rootView, SwipeRefreshLayout swipeRefreshLayout, SharedPreferences prefs, String fecha) {
        ViewGroup viewGroup = rootView.findViewById(R.id.linearLayout);
        viewGroup.removeAllViews();
        String[] favPeriodicos = prefs.getAll().values().toArray(new String[0]);
        GetPortadas getPts = new GetPortadas(rootView, getClass().getSimpleName(),fecha, swipeRefreshLayout);
        getPts.execute(favPeriodicos);
    }

}
