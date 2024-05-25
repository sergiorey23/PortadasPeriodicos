package sergirex.portadasperiodicos;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * Created by Sergio on 12/02/2017.
 *
 */

public class Locales extends Fragment {

    public static Locales newInstance(String fecha) {
        Bundle args = new Bundle();
        args.putString("fecha", fecha);
        Locales l = new Locales();
        l.setArguments(args);
        return l;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.portada_layout, container, false);
        String fecha = getArguments().getString("fecha");
        GetPortadas getPortadas = new GetPortadas(rootView, getClass().getSimpleName(), fecha,null);
        getPortadas.execute(Periodicos.locales);
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
        getPts.execute(Periodicos.locales);
    }
}
