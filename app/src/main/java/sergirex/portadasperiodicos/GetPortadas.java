package sergirex.portadasperiodicos;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.File;
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

class GetPortadas extends AsyncTask<String, Button, Boolean> {
    private final WeakReference<View> rootView;
    private final WeakReference<Context> context;
    private int portCont = 0;
    private WeakReference<LinearLayout> ly;
    private WeakReference<ProgressBar> pb;
    private final SharedPreferences fechasSP;
    private final SharedPreferences prefs;
    private String fecha;

    @SuppressLint("StaticFieldLeak")
    private final SwipeRefreshLayout swipeRefreshLayout;

    GetPortadas(View rootView, String simpleName, String fecha, SwipeRefreshLayout swipeRefreshLayout) {
        this.rootView = new WeakReference<>(rootView);
        context = new WeakReference<>(rootView.getContext());
        fechasSP = context.get().getSharedPreferences("Fechas" + simpleName, MODE_PRIVATE);
        prefs = PreferenceManager.getDefaultSharedPreferences(context.get());
        this.fecha = fecha;
        this.swipeRefreshLayout = swipeRefreshLayout;
    }

    View getRootView() {
        return rootView.get();
    }

    protected void onPreExecute() {
        ly = new WeakReference<>(rootView.get().findViewById(R.id.linearLayout));
        pb = new WeakReference<>(new ProgressBar(context.get()));
        ly.get().addView(pb.get());
    }

    @Override
    protected Boolean doInBackground(String... params) {

        final SavePortada savePortada = new SavePortada(context.get());

        String fechaAux = fecha;
        SharedPreferences.Editor editor = null;
        String fechaPortadas = fechasSP.getString("fechaPortadas", "");
        boolean descargar = !fechaPortadas.equals(fecha);

        if(descargar){
            editor = fechasSP.edit();
        }

        String siglaPais = "es";

        for (String periodico : params) {

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
                savePortada.saveFile(context.get().getCacheDir(),title + "t", portadaBM);
                if (editor != null) {
                    editor.putString(title, date);
                }
            }

            Portada portada = new Portada(periodico,title,fecha,webPeriodico,siglaPais);

            LinearLayout.LayoutParams paramsly = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            Button imageButton = new Button(context.get());
            paramsly.width = getHalfScreenWidth();
            paramsly.height = (portadaBM.getHeight() * paramsly.width) / portadaBM.getWidth();
            Drawable drawable = new BitmapDrawable(context.get().getResources(), portadaBM);
            imageButton.setBackground(drawable);
            imageButton.setLayoutParams(paramsly);

            if(!fechaAux.equals(date)){
                String[] arrayDate = date.split("/");
                imageButton.setText(String.format("%s/%s/%s", arrayDate[2], arrayDate[1], arrayDate[0]));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    imageButton.setTextAppearance(R.style.ButtonText);
                }
            }

            imageButton.setOnClickListener(view -> {
                Intent intent = new Intent(context.get(), PortadaDetalle.class);
                intent.putExtra("Portadas",params);
                intent.putExtra("selectedPortada", portada.getTitle());
                intent.putExtra("Fecha", fecha);
                int adCount = prefs.getInt("adCount", 0)+1;
                SharedPreferences.Editor edit = prefs.edit();
                edit.putInt("adCount",adCount);
                edit.apply();
                intent.putExtra("showAd", adCount);
                context.get().startActivity(intent);
            });
            String finalStrUrl = strUrl.replace(".640", "");
            imageButton.setOnLongClickListener(view -> {
                PopupMenu popup = new PopupMenu(view.getContext(), view);
                popup.getMenuInflater().inflate(R.menu.menu_portada_list, popup.getMenu());
                popup.setOnMenuItemClickListener(menuItem -> {
                    if (menuItem.getItemId() == R.id.share) {
                        if (savePortada.isExternalStorageWritable()) {
                            if (!savePortada.checkPermissions())
                                return false;
                            new DownloadPortada(context.get(), savePortada, portada.getTitle()).execute(finalStrUrl);
                        }
                    } else if (menuItem.getItemId() == R.id.save) {
                        if (savePortada.isExternalStorageWritable()) {
                            if (!savePortada.checkPermissions())
                                return false;
                            File file;
                            if ((file = new File(savePortada.getAlbumStorageDir() + File.separator + portada.getTitle() + "_" + (portada.getFecha() != null ? portada.getFecha().replace("/", "") : "") + ".jpg")).exists()) {
                                Toast.makeText(context.get(), "Ya se ha guardado la portada.", Toast.LENGTH_LONG).show();
                                return true;
                            }
                            DownloadPortada dp = new DownloadPortada(context.get(), file);
                            dp.execute(finalStrUrl);
                        } else {
                            Toast.makeText(context.get(), "Internal Storage unreadable", Toast.LENGTH_LONG).show();
                        }
                    } else if (menuItem.getItemId() == R.id.web) {
                        String url1 = portada.getWebPeriodico();
                        menuItem.setTitle(menuItem.getTitle() + url1);
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse("https://www." + url1));
                        context.get().startActivity(i);
                    }
                    return true;
                });
                MenuPopupHelper menuHelper = new MenuPopupHelper(context.get(), (MenuBuilder) popup.getMenu(), view);
                menuHelper.setForceShowIcon(true);
                menuHelper.show();
                return true;
            });

            publishProgress(imageButton);
        }
        if (descargar && editor != null) {
            editor.putString("fechaPortadas", fechaAux);
            editor.apply();
        }
        return true;
    }


    private WeakReference<LinearLayout> linearLayout;
    private int lycount = 0;

    protected void onProgressUpdate(Button... ib) {
        if (portCont % 2 == 0) {
            linearLayout = new WeakReference<>(new LinearLayout(context.get()));
            linearLayout.get().setOrientation(LinearLayout.HORIZONTAL);
            linearLayout.get().setHorizontalGravity(Gravity.CENTER_HORIZONTAL);
            linearLayout.get().addView(ib[0]);
            if (ly.get() != null)
                ly.get().addView(linearLayout.get(), lycount);
            lycount++;
        } else {
            if (linearLayout.get() != null) {
                Button button = (Button) linearLayout.get().getChildAt(0);
                LinearLayout.LayoutParams paramsly = (LinearLayout.LayoutParams) button.getLayoutParams();
                if(paramsly.height > ib[0].getLayoutParams().height){
                    ib[0].setLayoutParams(paramsly);
                }else{
                    paramsly.height = ib[0].getLayoutParams().height;
                    button.setLayoutParams(paramsly);
                }
                linearLayout.get().addView(ib[0]);
            }
        }
        portCont++;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        if (ly.get() != null) {
            ly.get().removeView(pb.get());
        }
        if(swipeRefreshLayout != null){
            swipeRefreshLayout.setRefreshing(false);
        }
        super.onPostExecute(aBoolean);
    }

    private static int getHalfScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels / 2;
    }
}

