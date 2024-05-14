package sergirex.portadasperiodicos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

/**
 * Created by Sergio on 12/02/2017.
 * Clase de periodicos generales
 */

public class General extends Fragment {

    private String fecha;
    public General(String fecha) {
        this.fecha = fecha;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.portada_layout, container, false);
        GetPortadas getPortadas = new GetPortadas(rootView, getClass().getSimpleName(),fecha);
        getPortadas.execute(Periodicos.general);
        return getPortadas.getRootView();
    }

}
