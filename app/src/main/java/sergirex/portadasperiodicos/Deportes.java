package sergirex.portadasperiodicos;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

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
        GetPortadas getPortadas = new GetPortadas(rootView, getClass().getSimpleName(), getArguments().getString("fecha"));
            getPortadas.execute(Periodicos.deportes);

        return getPortadas.getRootView();
    }
}
