package sergirex.portadasperiodicos;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by sergio on 01/10/2019.
 * Nothing else to add
 */
public class Favoritos extends Fragment {

    public static Favoritos newInstance(String fecha) {
        Bundle args = new Bundle();
        args.putString("fecha", fecha);
        Favoritos f = new Favoritos();
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.portada_layout, container, false);
        SharedPreferences prefs = rootView.getContext().getSharedPreferences("periodicos", Context.MODE_PRIVATE);
        if(prefs.getAll().isEmpty())
            return rootView;
        String fecha = getArguments().getString("fecha");
        GetPortadas getPortadas = new GetPortadas(rootView, getClass().getSimpleName(),fecha, null);
        String[] favPeriodicos = prefs.getAll().values().toArray(new String[0]);
        getPortadas.execute(favPeriodicos);
        SwipeRefreshLayout swipeRefreshLayout = rootView.findViewById(R.id.refreshLayout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshFragment(rootView, swipeRefreshLayout, prefs);
        });
        return getPortadas.getRootView();
    }

    private void refreshFragment(View rootView, SwipeRefreshLayout swipeRefreshLayout, SharedPreferences prefs) {
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
        String[] favPeriodicos = prefs.getAll().values().toArray(new String[0]);
        GetPortadas getPts = new GetPortadas(rootView, getClass().getSimpleName(),fecha, swipeRefreshLayout);
        getPts.execute(favPeriodicos);
    }

}
