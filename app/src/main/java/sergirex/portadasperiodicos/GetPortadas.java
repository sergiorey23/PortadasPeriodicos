package sergirex.portadasperiodicos;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class GetPortadas {

    /**
     * @param allPortadas   Needed for the intent
     * @param originalFecha Needed for the intent
     */ // Data class to hold the result for one newspaper cover
        public record PortadaResult(Portada portada, Bitmap bitmap, String finalDate,
                                    String finalImageUrl, String[] allPortadas, String originalFecha) {
    }

    // Listener to communicate with the UI thread
    public interface PortadasListener {
        void onPreExecute();
        void onPortadaLoaded(PortadaResult result);
        void onComplete();
    }

    private final WeakReference<Context> context;
    private final SharedPreferences fechasSP;
    private final String fecha;
    private final PortadasListener listener;

    @SuppressLint("StaticFieldLeak")
    private final SwipeRefreshLayout swipeRefreshLayout;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    GetPortadas(Context context, String simpleName, String fecha, SwipeRefreshLayout swipeRefreshLayout, PortadasListener listener) {
        this.context = new WeakReference<>(context);
        this.fechasSP = context.getSharedPreferences("Fechas" + simpleName, MODE_PRIVATE);
        this.fecha = fecha;
        this.swipeRefreshLayout = swipeRefreshLayout;
        this.listener = listener;
    }

    // This method now only does UI work on the main thread via listener
    protected void onPreExecute() {
        if (listener != null) {
            listener.onPreExecute();
        }
    }

    // This method now only does UI work on the main thread via listener
    protected void onPostExecute() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
        if (listener != null) {
            listener.onComplete();
        }
    }

    public void execute(String... params) {
        onPreExecute();
        executor.execute(() -> {
            doInBackground(params);
            handler.post(this::onPostExecute);
        });
    }

    private void doInBackground(String... params) {
        // FIX: Get context once and check for null to prevent crashes if the activity is destroyed.
        Context safeContext = context.get();
        if (safeContext == null) {
            return; // Activity is gone, abort the background task.
        }

        final SavePortada savePortada = new SavePortada(safeContext);

        SharedPreferences.Editor editor = null;
        String fechaPortadas = fechasSP.getString("fechaPortadas", "");
        boolean descargar = !fechaPortadas.equals(fecha);

        if (descargar) {
            editor = fechasSP.edit();
        }

        for (String periodico : params) {
            // FIX: Check context again inside the loop in case it gets destroyed during a long operation.
            if (context.get() == null) return;

            String siglaPais = "es";
            URL url;
            InputStream is = null;
            String webPeriodico;
            String title;
            String[] periodicoArray = periodico.split(":");

            if (periodicoArray.length > 1) {
                title = periodicoArray[0];
                webPeriodico = periodicoArray[1];
                if (periodicoArray.length == 3) {
                    siglaPais = periodicoArray[2];
                }
            } else {
                title = periodico.split("\\.")[0];
                webPeriodico = periodico;
            }
            Bitmap portadaBM;
            String strUrl = "";
            String date = fecha;

            if (!descargar && swipeRefreshLayout == null && fechasSP.getString(title, null) != null && (portadaBM = savePortada.getThumbFile(title + 't')) != null) {
                date = fechasSP.getString(title, fecha);
                strUrl = "https://img.kiosko.net/" + date + "/" + siglaPais + "/" + title + ".640.jpg";
            } else {
                Calendar calendar = Calendar.getInstance();
                DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd", Locale.FRANCE);
                try {
                    calendar.setTime(Objects.requireNonNull(formatter.parse(fecha)));
                } catch (ParseException e) {
                    calendar.setTime(new Date());
                }
                int count = 0;

                do {
                    try {
                        strUrl = "https://img.kiosko.net/" + date + "/" + siglaPais + "/" + title + ".640.jpg";
                        url = new URL(strUrl);
                        is = (InputStream) url.getContent();
                    } catch (FileNotFoundException fne) {
                        calendar.add(Calendar.DATE, -1);
                        date = formatter.format(calendar.getTime());
                        count++;
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                } while (is == null && count < 20);

                if (is == null) {
                    continue;
                }
                portadaBM = BitmapFactory.decodeStream(is);
                if (portadaBM != null) {
                    savePortada.saveFile(safeContext.getCacheDir(), title + "t", portadaBM);
                    if (editor != null) {
                        editor.putString(title, date);
                    }
                }
            }

            if (portadaBM == null) {
                continue;
            }

            Portada portada = new Portada(periodico, title, fecha, webPeriodico, siglaPais);

            final String finalDate = date;
            final String finalStrUrl = strUrl.replace(".640", "");

            if (listener != null) {
                PortadaResult result = new PortadaResult(portada, portadaBM, finalDate, finalStrUrl, params, fecha);
                handler.post(() -> listener.onPortadaLoaded(result));
            }
        }

        if (descargar && editor != null) {
            editor.putString("fechaPortadas", fecha);
            editor.apply();
        }
    }
}
