package sergirex.portadasperiodicos;

import android.app.ProgressDialog;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

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
    private static final String ARG_URL = "url";

    // TODO: Rename and change types of parameters
    private String title;
    private String url;

    public PortadaDetalleFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param title Parameter 1.
     * @param url Parameter 2.
     * @return A new instance of fragment PortadaDetalleFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PortadaDetalleFragment newInstance(String title, String url) {
        PortadaDetalleFragment fragment = new PortadaDetalleFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE);
            url = getArguments().getString(ARG_URL);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_portada_detalle, container, false);

        ImageView imageView = rootView.findViewById(R.id.imagen_extendida);
        ProgressDialog pd = new ProgressDialog(rootView.getContext());
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pd.setCancelable(false);
        pd.setIndeterminate(true);
        pd.show();
        Picasso.get().load(url)
                .into(imageView, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                        pd.cancel();
                        pd.dismiss();
                    }

                    @Override
                    public void onError(Exception e) {
                        pd.cancel();
                        pd.dismiss();
                        Picasso.get().load(url)
                                .into(imageView);
                    }
                });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        String title = this.title.replace("_", " ");
        title = title.substring(0, 1).toUpperCase() + title.substring(1);
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle(title);
    }
}