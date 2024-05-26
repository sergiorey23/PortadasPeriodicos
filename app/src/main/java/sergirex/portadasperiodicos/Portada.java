package sergirex.portadasperiodicos;

import java.io.Serializable;

/**
 * Created by Sergio on 08/06/2017.
 * Nothing else to add
 */

class Portada implements Serializable{
    private final String periodico;
    private final String title;
    private final String fecha;
    private final String webPeriodico;
    private final String siglaPais;

    Portada(String periodico, String title, String fecha, String webPeriodico, String siglaPais) {
        this.periodico = periodico;
        this.title = title;
        this.fecha = fecha;
        this.webPeriodico = webPeriodico;
        this.siglaPais = siglaPais;
    }

    public String getTitle() {
        return title;
    }

    public String getPeriodico() { return periodico; }

    public String getWebPeriodico() { return webPeriodico; }
    public String getSiglaPais() { return siglaPais; }
    public String getFecha() { return fecha; }
}
