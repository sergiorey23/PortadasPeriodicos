package sergirex.portadasperiodicos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/**
 * Created by Sergio on 12/02/2017.
 */

public class Internacional extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.portada_layout, container, false);
        GetPortadas portadasAsyncTask = new GetPortadas(rootView, getClass().getSimpleName());
        portadasAsyncTask.execute(Periodicos.internacional);

        return rootView.getRootView();
    }
}
