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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.portada_layout, container, false);
        GetPortadas getPortadas = new GetPortadas(rootView, getClass().getSimpleName());
            getPortadas.execute(Periodicos.economia);

            return getPortadas.getRootView();
    }
}
