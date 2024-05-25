package sergirex.portadasperiodicos;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * Created by Sergio on 12/02/2017.
 */

public class Internacional extends Fragment {

    public static Internacional newInstance(String fecha) {
        Bundle args = new Bundle();
        args.putString("fecha", fecha);
        Internacional i = new Internacional();
        i.setArguments(args);
        return i;
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.portada_layout, container, false);
        String fecha = getArguments().getString("fecha");
        GetPortadas portadasAsyncTask = new GetPortadas(rootView, getClass().getSimpleName(), fecha, null);
        portadasAsyncTask.execute(Periodicos.internacional);
        SwipeRefreshLayout swipeRefreshLayout = rootView.findViewById(R.id.refreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshFragment(rootView, swipeRefreshLayout, fecha);
        });
        return rootView.getRootView();
    }

    private void refreshFragment(View rootView, SwipeRefreshLayout swipeRefreshLayout, String fecha) {
        ViewGroup viewGroup = rootView.findViewById(R.id.linearLayout);
        viewGroup.removeAllViews();
        GetPortadas getPts = new GetPortadas(rootView, getClass().getSimpleName(),fecha, swipeRefreshLayout);
        getPts.execute(Periodicos.internacional);
    }
}
