package sergirex.portadasperiodicos;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/**
 * Created by sergio on 01/10/2019.
 * Nothing else to add
 */
public class Favoritos extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.portada_layout, container, false);
        SharedPreferences prefs = rootView.getContext().getSharedPreferences("periodicos", Context.MODE_PRIVATE);
        if(prefs.getAll().isEmpty())
            return rootView;
        GetPortadas getPortadas = new GetPortadas(rootView, getClass().getSimpleName());
        String[] favPeriodicos = prefs.getAll().values().toArray(new String[0]);
        getPortadas.execute(favPeriodicos);

        return getPortadas.getRootView();
    }

}
