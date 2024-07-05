package sergirex.portadasperiodicos;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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
        String fecha = getArguments().getString("fecha");
        GetPortadas getPortadas = new GetPortadas(rootView, getClass().getSimpleName(),fecha, null);
        getPortadas.execute(Periodicos.economia);
        SwipeRefreshLayout swipeRefreshLayout = rootView.findViewById(R.id.refreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshFragment(rootView, swipeRefreshLayout);
        });
        return getPortadas.getRootView();
    }
    private void refreshFragment(View rootView, SwipeRefreshLayout swipeRefreshLayout) {
        ViewGroup viewGroup = rootView.findViewById(R.id.linearLayout);
        viewGroup.removeAllViews();
        Calendar calendar = Calendar.getInstance();
        Date today = new Date();
        calendar.setTime(today);
        if(calendar.get(Calendar.HOUR_OF_DAY) < 6){
            calendar.add(Calendar.DATE, -1);
        }
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd", Locale.FRANCE);
        String fecha = formatter.format(calendar.getTime());
        GetPortadas getPts = new GetPortadas(rootView, getClass().getSimpleName(),fecha, swipeRefreshLayout);
        getPts.execute(Periodicos.economia);
    }
}
