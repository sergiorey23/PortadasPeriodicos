package sergirex.portadasperiodicos;

import java.io.Serializable;

/**
 * Created by Sergio on 08/06/2017.
 * Nothing else to add
 */

public record Portada(String periodico, String title, String fecha, String webPeriodico,
                      String siglaPais) implements Serializable {
}
