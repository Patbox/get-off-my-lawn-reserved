package draylar.goml.compat.webmap.player;

import org.jetbrains.annotations.Nullable;
import java.util.Base64;

/**
 * Stores a player head icon for web map display.
 */
public class PlayerHeadIcon {
    private final static String DEFAULT_ICON = "iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAIAAABLbSncAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAwklEQVQImWN0NlP79PkTMwPjr39///75x8TExMzCxM7GyfLl69dvX75eefSJAQZ05Pj+sP9lYvj/HyJ6aHLt7s4CBgaGK48+cXGwspRHhWzrKPr89RsLE+vnr58hmpIdrVg4WFi/ffvJ+fu7X8cMiOi8vFBuAWGWV4/vCvLwPXn3DiK6rTGV8dfHW7dfME5N9fn3/dOHP4x/GRiZGf7/ZWDkY/n79Q8Ty9N3H799+cTAwNC/4wIDA0NbpMP3P0x/GRgBboZUTsEWveAAAAAASUVORK5CYII=";

    private String rawIcon;
    private Boolean isBlank = false;


    /**
     * @param icon square PNG in base64 format or null to use default
     * @implNote Icon can also be an empty string which indicates an empty icon and all methods return an empty string
     */
    public PlayerHeadIcon(@Nullable String icon){
        if(icon == null) {
            this.rawIcon = DEFAULT_ICON;
        } else if (icon.isEmpty()) {
            this.isBlank = true;
            this.rawIcon = "";
        } else if (validatePng(icon)){
            this.rawIcon = icon;
        } else {
            throw new IllegalArgumentException("Error during icon creation: Not a valid base64 PNG image!");
        }
    }

    /**
     * @return Base64 PNG icon or an empty string if used during construction
     */
    public String getRaw(){
        return this.rawIcon;
    }    
    /**
     * @return an HTML img tag with base64 icon embedded, displayed at 8px
     * @implNote Returns an empty string if used during construction
     */
    public String getHtml(){
        return this.getHtml("16px", "");
    }
    /**
     * @param size horizontal/vertical size to display the image at
     * @return an HTML img tag with base64 icon embedded
     * @implNote Returns an empty string if used during construction
     */
    public String getHtml(String size){
        return this.getHtml(size, "");
    }
    /**
     * @param size horizontal/vertical size to display the image at
     * @param css additional CSS to add to the img tag
     * @return an HTML img tag with base64 icon embedded
     * @implNote Returns an empty string if used during construction
     */
    public String getHtml(String size, String css){
        return (this.isBlank) ? "" : 
        "<img alt=\"[icon]\" src=\"data:image/png;base64," + this.rawIcon + "\" width=\"" + size + "\" height=\"" + size + "\" style=\"vertical-align:middle;image-rendering:pixelated;" + css + "\" />";
    }

    private boolean validatePng(String raw){
        try {
            byte[] decoded = Base64.getDecoder().decode(raw);
            if (decoded.length < 8) {
                return false;
            }
            // Check PNG signature
            return decoded[0] == (byte) 0x89 &&
                   decoded[1] == (byte) 0x50 &&
                   decoded[2] == (byte) 0x4E &&
                   decoded[3] == (byte) 0x47 &&
                   decoded[4] == (byte) 0x0D &&
                   decoded[5] == (byte) 0x0A &&
                   decoded[6] == (byte) 0x1A &&
                   decoded[7] == (byte) 0x0A;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
