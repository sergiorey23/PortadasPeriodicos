package sergirex.portadasperiodicos;

import java.io.Serializable;

/**
 * Created by Sergio on 08/06/2017.
 * Nothing else to add
 */

class Portada implements Serializable{
    private String title;
    private String webPeriodico;
    private String urlPortada;
    private String periodico;

    Portada(String title, String webPeriodico, String urlPortada, String periodico) {
        this.title = title;
        this.webPeriodico = webPeriodico;
        this.urlPortada = urlPortada;
        this.periodico = periodico;
    }

    String getTitle() {
        return title;
    }

    String getPeriodico() { return periodico; }

    String getWebPeriodico() { return webPeriodico; }

    String getUrlPortada() { return urlPortada; }

}
