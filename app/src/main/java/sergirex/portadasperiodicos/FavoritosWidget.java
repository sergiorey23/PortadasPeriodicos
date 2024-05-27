package sergirex.portadasperiodicos;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.RemoteViews;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Implementation of App Widget functionality.
 */
public class FavoritosWidget extends AppWidgetProvider {
    private final String INDEX_SP = "IndiceWDT";
    public String LEFT_BTN = "android.appwidget.action.LEFT";
    public String RIGHT_BTN = "android.appwidget.action.RIGHT";
    public static String[] periodicos = {"elpais.com", "elmundo.es", "abc.es", "larazon.es", "lavanguardia.com", "elperiodico.com"
            , "marca.com", "as.com", "sport.es", "mundodeportivo.com", "expansion.com", "eleconomista.es", "diario_informacion:informacion.es",
            "levante:levante-emv.com", "diario_montanes:eldiariomontanes.es", "elcorreo:elcorreo.com", "diario_navarra:diariodenavarra.es",
            "lanuevaespana:lne.es", "diario_cordoba:diariocordoba.es", "eldia_cordoba:eldiadecordoba.es", "granada_hoy:granadahoy.com", "ideal_jaen:ideal.es",
            "larioja:larioja.com", "diario_leon:diariodeleon.es", "lasprovincias:lasprovincias.es", "farovigo:farodevigo.es",
            "laprovincia:laprovincia.es", "latribuna_ciudadreal:latribunadeciudadreal.es", "canarias7:canarias7.es",
            "diario_mallorca:diariodemallorca.es", "laverdad_murcia:laverdad.es", "heraldo_aragon:heraldo.es",
            "newyork_times:nytimes.com/es:us", "wsj:wsj.com:us", "the_times:thetimes.co.uk:uk", "guardian:theguardian.com/uk:uk",
            "ft_us:ft.com:us", "washington_post:washingtonpost.com:us"};
    public int index;
    public SharedPreferences prefs;
    public SharedPreferences.Editor editor;
    //public Portada portada;
    public RemoteViews views;

    void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                         int appWidgetId) {
        // Construct the RemoteViews object
        prefs = context.getSharedPreferences(INDEX_SP, Context.MODE_PRIVATE);
        index = prefs.getInt("index", 0);
        if (views == null) {
            views = new RemoteViews(context.getPackageName(), R.layout.favoritos_widget);
        }

        Calendar calendar = Calendar.getInstance();
        Date today = new Date();
        calendar.setTime(today);
        if (calendar.get(Calendar.HOUR_OF_DAY) < 6) {
            calendar.add(Calendar.DATE, -1);
        }
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd", Locale.FRANCE);
        String fecha = formatter.format(calendar.getTime());

        String fechaPortadas = context.getSharedPreferences("FechasGeneral", Context.MODE_PRIVATE).getString("fechaPortadas", null);

        if(fechaPortadas == null || !fechaPortadas.equals(fecha)){
            fecha = null;
        }

        views.setViewVisibility(R.id.rightBtn, View.VISIBLE);
        views.setViewVisibility(R.id.leftBtn, View.VISIBLE);
        Intent intentLeftBtn = new Intent(context, FavoritosWidget.class);
        Intent intentRightBtn = new Intent(context, FavoritosWidget.class);

        intentLeftBtn.setAction(LEFT_BTN);
        intentLeftBtn.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intentLeftBtn.putExtra("fecha",fecha);
        intentRightBtn.setAction(RIGHT_BTN);
        intentRightBtn.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intentRightBtn.putExtra("fecha",fecha);

        PendingIntent pendingIntentLeftBtn = PendingIntent.getBroadcast(context, 0, intentLeftBtn, PendingIntent.FLAG_IMMUTABLE);
        PendingIntent pendingIntentRightBtn = PendingIntent.getBroadcast(context, 0, intentRightBtn, PendingIntent.FLAG_IMMUTABLE);

        views.setOnClickPendingIntent(R.id.leftBtn, pendingIntentLeftBtn);
        views.setOnClickPendingIntent(R.id.rightBtn, pendingIntentRightBtn);

        views.setImageViewBitmap(R.id.imageViewWidget, getPortada(index, context.getCacheDir().getAbsolutePath(), fecha));

        Intent intentApp = new Intent(context, Portadas.class);
        PendingIntent pendingIntentAbrirApp = PendingIntent.getActivity(context, 0, intentApp, PendingIntent.FLAG_IMMUTABLE);

        views.setOnClickPendingIntent(R.id.imageViewWidget, pendingIntentAbrirApp);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    public Bitmap getPortada(int index, String path, String fecha) {

        String siglaPais = "es";
        String periodico = periodicos[index];
        String[] periodicoArray = periodico.split(":");
        String title;
        if (periodicoArray.length > 1) {
            title = periodicoArray[0];
            if (periodicoArray.length == 3) {
                siglaPais = periodicoArray[2];
            }
        } else {
            title = periodico.split("\\.")[0];
        }

        File file;
        if(fecha != null && (file = new File( path+ File.separator + title + "t.png")).exists()){
            return BitmapFactory.decodeFile(file.getPath());
        }

        Calendar calendar = Calendar.getInstance();
        Date today = new Date();
        calendar.setTime(today);
        if (calendar.get(Calendar.HOUR_OF_DAY) < 4) {
            calendar.add(Calendar.DATE, -1);
        }
        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd", Locale.FRANCE);
        fecha = formatter.format(calendar.getTime());


        InputStream is = null;
        URL url;
        int count = 0;
        do {
            try {
                String strUrl = "https://img.kiosko.net/" + fecha + "/" + siglaPais + "/" + title + ".640.jpg";
                url = new URL(strUrl);
                is = (InputStream) url.getContent();
            } catch (FileNotFoundException fne) {
                calendar.add(Calendar.DATE, -1);
                fecha = formatter.format(calendar.getTime());
                count++;
            } catch (MalformedURLException e) {
                e.printStackTrace();
                break;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        } while (is == null && count < 20);
        if (is == null) return null;
        return BitmapFactory.decodeStream(is);
    }


    @Override
    public void onReceive(final Context context, final Intent intent) {
        super.onReceive(context, intent);
        prefs = context.getSharedPreferences(INDEX_SP, Context.MODE_PRIVATE);
        index = prefs.getInt("index", 0);
        final String fecha = intent.getStringExtra("fecha");
        if (LEFT_BTN.equals(intent.getAction())) {
            if (index == 0)
                return;
            new Thread(() -> {
                try {
                    if (views == null) {
                        views = new RemoteViews(context.getPackageName(), R.layout.favoritos_widget);
                    }
                    if (prefs != null) {
                        editor = prefs.edit();
                        editor.putInt("index", --index);
                        editor.apply();
                    }
                    views.setImageViewBitmap(R.id.imageViewWidget, getPortada(index, context.getCacheDir().getAbsolutePath(), fecha));
                    Intent intentApp = new Intent(context, Portadas.class);
                    PendingIntent pendingIntentAbrirApp = PendingIntent.getActivity(context, 0, intentApp, PendingIntent.FLAG_IMMUTABLE);
                    views.setOnClickPendingIntent(R.id.imageViewWidget, pendingIntentAbrirApp);
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                    appWidgetManager.updateAppWidget(intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID), views);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

        } else if (RIGHT_BTN.equals(intent.getAction())) {
            if (periodicos.length == index + 1)
                return;
            new Thread(() -> {
                try {
                    if (views == null) {
                        views = new RemoteViews(context.getPackageName(), R.layout.favoritos_widget);
                    }
                    if (prefs != null) {
                        editor = prefs.edit();
                        editor.putInt("index", ++index);
                        editor.apply();
                    }
                    views.setImageViewBitmap(R.id.imageViewWidget, getPortada(index, context.getCacheDir().getAbsolutePath(), fecha));
                    Intent intentApp = new Intent(context, Portadas.class);
                    PendingIntent pendingIntentAbrirApp = PendingIntent.getActivity(context, 0, intentApp, PendingIntent.FLAG_IMMUTABLE);
                    views.setOnClickPendingIntent(R.id.imageViewWidget, pendingIntentAbrirApp);
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                    appWidgetManager.updateAppWidget(intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID), views);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (final int appWidgetId : appWidgetIds) {
            Thread thread = new Thread(() -> {
                try {
                    updateAppWidget(context, appWidgetManager, appWidgetId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            thread.start();
        }
    }
}

