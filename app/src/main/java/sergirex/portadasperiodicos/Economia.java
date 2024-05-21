package sergirex.portadasperiodicos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

/**
 * Created by Sergio on 12/02/2017.
 */

public class Economia extends Fragment {

    public static Economia newInstance(String fecha) {
        Bundle args = new Bundle();
        args.putString("fecha", fecha);
        Economia e = new Economia();
        e.setArguments(args);
        return e;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.portada_layout, container, false);
        GetPortadas getPortadas = new GetPortadas(rootView, getClass().getSimpleName(),getArguments().getString("fecha"));
            getPortadas.execute(Periodicos.economia);

            return getPortadas.getRootView();
    }
}
