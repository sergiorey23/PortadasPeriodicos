package sergirex.portadasperiodicos;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * Created by Sergio on 12/02/2017.
 * Clase de periodicos generales
 */

public class General extends Fragment {

    public static General newInstance(String fecha) {
        Bundle args = new Bundle();
        args.putString("fecha", fecha);
        General g = new General();
        g.setArguments(args);
        return g;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.portada_layout, container, false);
        String fecha = getArguments().getString("fecha");
        GetPortadas getPortadas = new GetPortadas(rootView, getClass().getSimpleName(),fecha,null);
        getPortadas.execute(Periodicos.general);
        SwipeRefreshLayout swipeRefreshLayout = rootView.findViewById(R.id.refreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshFragment(rootView,swipeRefreshLayout,fecha);
            swipeRefreshLayout.setRefreshing(false);
        });

        return getPortadas.getRootView();
    }

    private void refreshFragment(View rootView, SwipeRefreshLayout swipeRefreshLayout, String fecha) {
        ViewGroup viewGroup = rootView.findViewById(R.id.linearLayout);
        viewGroup.removeAllViews();
        GetPortadas getPts = new GetPortadas(rootView, getClass().getSimpleName(),fecha, swipeRefreshLayout);
        getPts.execute(Periodicos.general);
    }

}
