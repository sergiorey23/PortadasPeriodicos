package sergirex.portadasperiodicos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

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
        GetPortadas getPortadas = new GetPortadas(rootView, getClass().getSimpleName(), getArguments().getString("fecha"));

        getPortadas.execute(Periodicos.locales);

            return getPortadas.getRootView();
    }
}
