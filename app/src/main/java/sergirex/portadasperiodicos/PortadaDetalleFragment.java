package sergirex.portadasperiodicos;

import android.app.ProgressDialog;
import android.media.Image;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PortadaDetalleFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PortadaDetalleFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_TITLE = "title";
    private static final String ARG_SIGLA_PAIS = "sigla_pais";
    private static final String ARG_FECHA = "fecha";

    // TODO: Rename and change types of parameters
    private String title;
    private String siglaPais;
    private String fecha;
    //private ImageView imageView;
    private ProgressDialog pd;

    public PortadaDetalleFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param title Parameter 1.
     * @param siglaPais Parameter 2.
     * @param fecha Parameter 3.
     * @return A new instance of fragment PortadaDetalleFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PortadaDetalleFragment newInstance(String title, String siglaPais, String fecha) {
        PortadaDetalleFragment fragment = new PortadaDetalleFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_SIGLA_PAIS, siglaPais);
        args.putString(ARG_FECHA, fecha);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE);
            siglaPais = getArguments().getString(ARG_SIGLA_PAIS);
            fecha = getArguments().getString(ARG_FECHA);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_portada_detalle, container, false);

        pd = new ProgressDialog(rootView.getContext(),R.style.DialogCustom);
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.setCancelable(false);
        pd.setMessage(getString(R.string.loading));
        pd.setIndeterminate(true);
        pd.show();
        getCover(rootView);
        return rootView;
    }

    private int tries = 0;
    public void getCover(View rootView){
        if (tries < 20) {
            ImageView imageView = rootView.findViewById(R.id.imagen_extendida);
            String url = "https://img.kiosko.net/" + fecha + "/" + siglaPais + "/" + title + ".jpg";
            Picasso.get().load(url)
                    .into(imageView, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            pd.cancel();
                            pd.dismiss();
                            Calendar calendar = Calendar.getInstance();
                            Date today = new Date();
                            calendar.setTime(today);
                            if (calendar.get(Calendar.HOUR_OF_DAY) < 6) {
                                calendar.add(Calendar.DATE, -1);
                            }
                            DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd", Locale.FRANCE);
                            if(!Objects.equals(fecha, formatter.format(calendar.getTime()))){
                                TextView textView = rootView.findViewById(R.id.fechaPortada);
                                String[] dateStr = fecha.split("/");
                                textView.setText(String.format("%s/%s/%s", dateStr[2], dateStr[1], dateStr[0]));
                                textView.setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            tries++;
                            DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd", Locale.FRANCE);
                            try {
                                Date date = formatter.parse(fecha);
                                if(date == null){
                                    throw new ParseException(e.getMessage(),0);
                                }
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(date);
                                calendar.add(Calendar.DATE, -1);
                                fecha = formatter.format(calendar.getTime());
                                getCover(rootView);
                            } catch (ParseException ex) {
                                Toast.makeText(getActivity(), ex.getMessage(), Toast.LENGTH_LONG).show();
                                tries = 10;
                            }
                        }
                    });
        }else{
            Toast.makeText(getActivity(), "Couldn't find any cover", Toast.LENGTH_LONG).show();
            pd.cancel();
            pd.dismiss();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        String title = this.title.replace("_", " ");
        title = title.substring(0, 1).toUpperCase() + title.substring(1);
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(title);
    }
}