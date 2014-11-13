/*
    plugin : DistToPath_.java
    author : Max Larsson
    e-mail : max.larsson@liu.se

    This ImageJ plugin is for use in conjunction with DistToPath.py.
    
    Copyright 2001-2014 Max Larsson <max.larsson@liu.se>

    This software is released under the MIT license.
      
*/

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Random;
import ij.*;
import ij.IJ;
import ij.ImagePlus;
import ij.io.*;
import ij.gui.*;
import ij.plugin.frame.*;
import ij.measure.*;


interface VersionDTP {
    String title = "DistToPath";
    String author = "Max Larsson";
    String version = "1.0.0";
    String year = "2014";
    String month = "November";
    String day = "11";
    String email = "max.larsson@liu.se";
    String homepage = "http://www.hu.liu.se/forskning/larsson-max/software";
}

interface OptionsDTP {
    Color pathCol = Color.blue;
    Color pointCol = Color.green;
    Color pospolCol = Color.magenta;
    Color randomCol = Color.yellow;
    Color holeCol = Color.red;
    Color textCol = Color.blue;
}

public class DistToPath_ extends PlugInFrame implements OptionsDTP, ActionListener {
    
    Panel panel;
    static Frame instance;
    static Frame infoFrame;
    GridBagLayout infoPanel;
    GridBagConstraints c;    
    Label profile_nLabel;
    Label pathnLabel;
    Label pnLabel;    
    Label pospolLabel;
    Label holenLabel;            
    Label randomPlacedLabel;
    Label commentLabel;
    Label scaleLabel;
    ProfileDataDTP profile;
    ImagePlus imp;


    public DistToPath_() {
        super("DistToPath");
        if (instance != null) {
            instance.toFront();
            return;
        }
        instance = this;
        profile = new ProfileDataDTP();
        IJ.register(DistToPath_.class);
        setLayout(new FlowLayout());
        setBackground(SystemColor.control);
        panel = new Panel();
        panel.setLayout(new GridLayout(0, 1, 4, 1));
        panel.setBackground(SystemColor.control);    
        panel.setFont(new Font("Helvetica", 0, 12));
        addButton("Save profile");
        addButton("Clear profile");
        panel.add(new Label(""));
        panel.add(new Label("Define selection as:"));                
        addButton("Path");
        addButton("Points");
        addButton("Positive polarity");
        addButton("Hole");        
        panel.add(new Label(""));
        addButton("Place random points");
        panel.add(new Label(""));
        panel.add(new Label("Delete profile components:"));
        addButton("Delete path");
        addButton("Delete points");
        addButton("Delete polarity");
        addButton("Delete random points");
        addButton("Delete selected component");
        panel.add(new Label(""));
        panel.add(new Label("Other:"));                
        addButton("Add comment");
        addButton("Set profile n");
        addButton("Options...");
        addButton("About...");
        add(panel);
        pack();
        setVisible(true);
        infoFrame = new Frame("Profile info");
        infoPanel = new GridBagLayout();
        infoFrame.setFont(new Font("Helvetica", 0, 10));
        infoFrame.setBackground(SystemColor.control);
        infoFrame.setLocation(0, instance.getLocation().x + instance.getSize().height + 3);
        infoFrame.setIconImage(instance.getIconImage());
        infoFrame.setResizable(false);
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        addStaticInfoLabel("Profile n:");
        profile_nLabel = new Label(IJ.d2s(profile.ntot, 0), Label.RIGHT);
        addVarInfoLabel(profile_nLabel);
        addStaticInfoLabel("Points:");
        pnLabel = new Label("0", Label.RIGHT);
        addVarInfoLabel(pnLabel);
        addStaticInfoLabel("Path nodes:");
        pathnLabel = new Label("0", Label.RIGHT);
        addVarInfoLabel(pathnLabel);
        addStaticInfoLabel("Positive polarity:");
        pospolLabel = new Label("N/D", Label.RIGHT);
        addVarInfoLabel(pospolLabel);
        addStaticInfoLabel("Holes:");
        holenLabel = new Label("0", Label.RIGHT);
        addVarInfoLabel(holenLabel);
        addStaticInfoLabel("Random points:");
        randomPlacedLabel = new Label("no", Label.RIGHT);
        addVarInfoLabel(randomPlacedLabel);
        addStaticInfoLabel("Pixel width:");
        scaleLabel = new Label("N/D", Label.RIGHT);
        addVarInfoLabel(scaleLabel);
        addStaticInfoLabel("Comment:");
        commentLabel = new Label("", Label.RIGHT);
        addVarInfoLabel(commentLabel);
        infoFrame.setLayout(infoPanel);
        infoFrame.pack();
        infoFrame.setVisible(true);
        infoFrame.setSize(instance.getSize().width, infoFrame.getSize().height);
        instance.requestFocus();
    }

    void addButton(String label) {
        Button b = new Button(label);
        b.addActionListener(this);
        panel.add(b);
    }

    void addStaticInfoLabel(String label) {
        Label l = new Label(label, Label.LEFT);
        c.gridwidth = 1;
        infoPanel.setConstraints(l, c);
        infoFrame.add(l);
    }

    void addVarInfoLabel(Label l) {
        c.gridwidth = GridBagConstraints.REMAINDER;
        infoPanel.setConstraints(l, c);
        infoFrame.add(l);
    }

    PolygonRoi getPolylineRoi(ImagePlus imp) {
        Roi roi = imp.getRoi();
        if (roi == null || roi.getType() != Roi.POLYLINE) {
            IJ.error("DistToPath", "Segmented line selection required.");
            return null;
        } else {
            return (PolygonRoi) roi;
        }
    }

    PolygonRoi getPolygonRoi(ImagePlus imp) {
        Roi roi = imp.getRoi();
        if (roi == null || roi.getType() != Roi.POLYGON) {
            IJ.error("DistToPath", "Polygon selection required.");
            return null;
        } else {
            return (PolygonRoi) roi;
        }
    }

    PolygonRoi getPointRoi(ImagePlus imp) {
        Roi roi = imp.getRoi();
        if (roi == null || roi.getType() != Roi.POINT) {
            IJ.error("DistToPath", "Point selection required.");
            return null;
        } else {
            return (PolygonRoi) roi;
        }
    }

    void updateInfoPanel() {
        double pixelwidth;
        String unit;

        profile_nLabel.setText(IJ.d2s(profile.ntot, 0));
        pnLabel.setText(IJ.d2s(profile.getNumPoints("points"), 0));
        pathnLabel.setText(IJ.d2s(profile.getNumPoints("path"), 0));
        holenLabel.setText(IJ.d2s(profile.getNum("hole"), 0));
        if (profile.overlay.getIndex("positive polarity") != -1) {
            pospolLabel.setText("yes");
        } else {
            pospolLabel.setText("no");
        }        
        if (profile.overlay.getIndex("randon points") != -1) {
            randomPlacedLabel.setText("yes");
        } else {
            randomPlacedLabel.setText("no");
        }
        Calibration c = imp.getCalibration();
        if (c.getUnit().equals("micron")) {
            pixelwidth = c.pixelWidth * 1000;
            unit = "nm";
        } else {
            pixelwidth = c.pixelWidth;
            unit = c.getUnit();
        }
        scaleLabel.setText(IJ.d2s(pixelwidth, 2) + " " + unit);
        commentLabel.setText(profile.comment);
    }

    public boolean isImage(ImagePlus imp) {
        if (imp == null) {
            IJ.beep();
            IJ.showStatus("No image");
            return false;
        }
        return true;
    }

    public void actionPerformed(ActionEvent e) {
        PolygonRoi p;
        Polygon randomPol;
        PointRoi randomRoi;
        int i, x, y;
        String s;

        String command = e.getActionCommand();
        if (command == null) {
            return;
        }
        imp = WindowManager.getCurrentImage();
        imp.setOverlay(profile.overlay);
        if (imp != null && imp.getType() != ImagePlus.COLOR_RGB) {
            imp.setProcessor(imp.getTitle(), imp.getProcessor().convertToRGB());
        }
        if (command.equals("Save profile")) {
            if (!isImage(imp)) {
                return;
            }
            if (!profile.dirty) {
                IJ.showMessage("Nothing to save.");
            } else {
                boolean saved = profile.save(imp);
                if (saved) {
                    profile.clear();
                }
            }
        }
        if (command.equals("Clear profile")) {
            if (!isImage(imp)) {
                return;
            }
            if (profile.dirty) {
                YesNoCancelDialog d = new YesNoCancelDialog(imp.getWindow(),
                        "DistToPath", "Save current\nprofile?");
                if (d.yesPressed()) {
                    profile.dirty = !profile.save(imp);
                } else if (!d.cancelPressed()) {
                    profile.dirty = false;
                }
            }
            if (!profile.dirty) {
                profile.clear();
                IJ.showStatus("Profile cleared.");
            }
        }
        if (command.equals("Path")) {
            if (!isImage(imp) || !profile.isSameImage(imp) ||
                    profile.isDefined("path", "Path")) {
                return;
            }
            if ((p = getPolylineRoi(imp)) != null) {
                p.setName("path");
                p.setStrokeColor(pathCol);
                profile.overlay.add(p);
                profile.dirty = true;
            }
        }
        if (command.equals("Points")) {
            if (!isImage(imp) || !profile.isSameImage(imp) ||
                    profile.isDefined("points", "Points")) {
                return;
            }
            if ((p = getPointRoi(imp)) != null) {
                p.setName("points");
                p.setStrokeColor(pointCol);
                profile.overlay.add(p);
                profile.dirty = true;
            }
        }
        if (command.equals("Positive polarity")) {
            if (!isImage(imp) || !profile.isSameImage(imp) ||
                    profile.overlay.getIndex("positive polarity") != -1) {
                return;
            }
            if ((p = getPointRoi(imp)) != null) {
                if (p.getNCoordinates() > 1) {
                    IJ.error("DistToPath", "Could not define polarity:\nMore than one point selected.");
                    return;
                }
                p.setName("positive polarity");
                p.setStrokeColor(pospolCol);
                profile.overlay.add(p);
                profile.dirty = true;
            }
        }
        if (command.equals("Hole")) {
            if (!isImage(imp) || !profile.isSameImage(imp)) {
                return;
            }
            if ((p = getPolygonRoi(imp)) != null) {
                p.setName("hole");
                p.setStrokeColor(holeCol);
                profile.overlay.add(p);
                profile.dirty = true;
            }
        }
        if (command.equals("Place random points")) {
            if (!isImage(imp) || !profile.isSameImage(imp) ||
                    profile.overlay.getIndex("randon points") != -1) {
                return;
            }
            Random rnd = new Random();
            randomPol = new Polygon();
            for (i = 0; i < profile.randompn; i++) {
                x = rnd.nextInt(imp.getWidth() - 1) + 1;
                y = rnd.nextInt(imp.getHeight() - 1) + 1;
                randomPol.addPoint(x, y);
            }
            randomRoi = new PointRoi(randomPol);
            randomRoi.setHideLabels(true);
            randomRoi.setName("randon points");
            randomRoi.setStrokeColor(randomCol);
            profile.overlay.add(randomRoi);
        }
        if (command.equals("Delete path")) {
            if (!isImage(imp) || !profile.isSameImage(imp)) {
                return;
            }
            profile.deleteNamedComponent(imp, "path");
        }
        if (command.equals("Delete points")) {
            if (!isImage(imp) || !profile.isSameImage(imp)) {
                return;
            }
            profile.deleteNamedComponent(imp, "points");
        }
        if (command.equals("Delete polarity")) {
            if (!isImage(imp) || !profile.isSameImage(imp)) {
                return;
            }
            profile.deleteNamedComponent(imp, "positive polarity");
        }
        if (command.equals("Delete random points")) {
            if (!isImage(imp) || !profile.isSameImage(imp)) {
                return;
            }
            profile.deleteNamedComponent(imp, "random points");
        }
        if (command.equals("Delete selected component")) {
            if ((imp.getRoi()) != null) {
                profile.deleteSelectedComponent(imp);
            }
        }
        if (command.equals("Set profile n")) {
            s = IJ.getString("Set profile n", IJ.d2s(profile.ntot, 0));
            profile.ntot = java.lang.Integer.parseInt(s);
        }
        if (command.equals("Add comment")) {
            s = IJ.getString("Comment: ", profile.comment);
            if (!s.equals("")) {
                profile.comment = s;
                profile.dirty = true;
            }
        }
        if (command.equals("Options...")) {
            GenericDialog gd = new GenericDialog("Options");
            gd.setInsets(0, 0, 0);
            gd.addMessage("Random particles:");
            gd.addNumericField("Random particle n:", profile.randompn, 0);
            gd.showDialog();
            if (gd.wasCanceled())
                return;
            profile.randompn = (int) gd.getNextNumber();
            if (profile.randompn <=0) {
                IJ.error("Random point n must be larger than 0. Reverting to default value (40).");
                profile.randompn = 40;
            }
        }
        if (command.equals("About...")) {
            String aboutHtml = String.format("<html><p><strong>%s" +
                            "</strong></p><br />" +
                            "<p>Version %s</p><br />" +
                            "<p>Last modified %s %s, %s.</p>" +
                            "<p> Copyright 2001 - %s %s.</p>" +
                            "<p> Released under the MIT license. " +
                            "</p><br />" +
                            "<p>E-mail: %s</p>" +
                            "<p>Web: %s</p><br /></html>",
                    VersionDTP.title,
                    VersionDTP.version,
                    VersionDTP.month,
                    VersionDTP.day,
                    VersionDTP.year,
                    VersionDTP.year,
                    VersionDTP.author,
                    VersionDTP.email,
                    VersionDTP.homepage);
            new HTMLDialog(VersionDTP.title, aboutHtml);
        }
        updateInfoPanel();
        imp.updateAndDraw();
        IJ.showStatus("");
    }

    public void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID()==WindowEvent.WINDOW_CLOSING) {
            infoFrame.dispose();
            infoFrame = null;
            instance = null;
        }
    }


} // end of DistToPath_


class ProfileDataDTP implements OptionsDTP {
    boolean dirty;
    Overlay overlay;
    int n, ntot, randompn, i;
    int imgID;
    String ID, comment, prevImg;

    ProfileDataDTP() {
        this.n = 0;
        this.ntot = 1;
        this.prevImg = "";
        this.imgID = 0;
        this.dirty = false;
        this.overlay = new Overlay();
        this.randompn = 200;
        this.comment = "";
        this.ID = "";
    }


    // Returns number of ROIs named 'name' in the overlay. Returns 0 if ROI not found in overlay.
    public int getNum(String name) {
        int i, n=0;

        for (i = 0; i < this.overlay.size(); i++) {
            if (this.overlay.get(i).getName().equals(name)) {
                n++;
            }
        }
        return n;
    }
    // Returns number of points in ROI named 'name' in the overlay. Returns 0 if ROI not found in overlay.
    public int getNumPoints(String name) {
        if (this.overlay.getIndex(name) == -1) {
            return 0;
        } else {
            return this.overlay.get(this.overlay.getIndex(name)).getPolygon().npoints;
        }
    }

    public void deleteSelectedComponent(ImagePlus imp) {
        if (!this.overlay.contains(imp.getRoi())) {
            IJ.error("The current selection does not define a profile component.");
        } else {
            YesNoCancelDialog d = new YesNoCancelDialog(imp.getWindow(),
                    "DistToPath", "Delete " + imp.getRoi().getName() + "?");
            if (d.yesPressed()) {
                this.overlay.remove(imp.getRoi());
                imp.deleteRoi();
            }
        }
    }

    public void deleteNamedComponent(ImagePlus imp, String name) {
        if (this.overlay.getIndex(name) == -1) {
            IJ.error("No " + name + " defined.");
        } else {
            YesNoCancelDialog d = new YesNoCancelDialog(imp.getWindow(),
                    "DistToPath", "Delete " + name + "?");
            if (d.yesPressed()) {
                this.overlay.remove(this.overlay.getIndex(name));
            }
        }
    }

    public boolean isSameImage(ImagePlus imp) {
        if (!this.dirty || this.imgID == 0) {
            this.imgID = imp.getID();
            return true;
        } else if (this.imgID == imp.getID()) {
            return true;
        } else {
            IJ.error("DistToPath", "All measurements must be performed on the same image.");
            return false;
        }
    }

    public boolean isDefined(String name, String errstr) {
        if (this.overlay.getIndex(name) != -1) {
            IJ.error(errstr + " already defined. Please delete old instance first.");
            return true;
        }
        return false;
    }

    private boolean CheckProfileData(ImagePlus imp) {
        String[] warnstr, errstr;
        int i, nwarn = 0, nerr = 0;

        warnstr = new String[9];
        errstr = new String[9];
        Calibration c = imp.getCalibration();
        if (c.getUnit().equals(" ") || c.getUnit().equals("inch")) {
            errstr[nerr++] = "It appears the scale has not been set.";
        }
        if (this.getNumPoints("path") == 0) {
            errstr[nerr++] = "Path not defined.";
        }
        if (this.getNumPoints("points") == 0) {
            warnstr[nwarn++] = "No point coordinates defined.";
        }
        if (this.overlay.getIndex("positive polarity") == -1) {
            warnstr[nwarn++] = "Positive polarity not defined.";
        }
        if (nerr > 0) {
            IJ.error("DistToPath", "Error:\n" + errstr[0]);
            return false;
        }
        if (nwarn > 0) {
            for (i = 0; i < nwarn; i++) {
                YesNoCancelDialog d = new YesNoCancelDialog(imp.getWindow(),
                        "DistToPath", "Warning:\n" + warnstr[i] + "\nContinue anyway?");
                if (!d.yesPressed()) {
                    return false;
                }
            }
        }
        return true;
    }


    public boolean save(ImagePlus imp) {
        int i;
        double pixelwidth;
        String s, unit;
        Polygon pol;

        IJ.showStatus("Saving profile...");
        if (!CheckProfileData(imp)) {
            return false;
        }
        Calibration c = imp.getCalibration();
        if (c.pixelWidth != c.pixelHeight) {
            IJ.showMessage("Warning: pixel aspect ratio is not 1.\n" +
                    "Only pixel WIDTH is used.");
        }
        try {
            if (!imp.getTitle().equals(this.prevImg)) {
                this.n = 0;
                this.prevImg = imp.getTitle();
            }
            this.n++;
            s = IJ.getString("Profile ID: ", IJ.d2s(this.ntot, 0));
            if (!s.equals("")) {
                this.ID = s;
            }
            SaveDialog sd = new SaveDialog("Save profile",
                    imp.getTitle() + "." +
                            IJ.d2s(this.n,0), ".dtp");
            if (sd.getFileName() == null) {
                this.n--;
                return false;
            }
            PrintWriter outf =
                    new PrintWriter(
                            new BufferedWriter(
                                    new FileWriter(sd.getDirectory() +
                                            sd.getFileName())));
            String versionInfo = String.format("# %s version %s (%s %s, %s)",
                    VersionDTP.title,
                    VersionDTP.version,
                    VersionDTP.month,
                    VersionDTP.day,
                    VersionDTP.year);
            outf.println(versionInfo);
            outf.println("IMAGE " + imp.getTitle());
            outf.println("PROFILE_ID " + this.ID);
            if (!this.comment.equals("")) {
                outf.println("COMMENT " + this.comment);
            }
            if (c.getUnit().equals("micron")) {
                pixelwidth = c.pixelWidth * 1000;
                unit = "nm";
            } else {
                pixelwidth = c.pixelWidth;
                unit = c.getUnit();
            }
            outf.println("PIXELWIDTH " + IJ.d2s(pixelwidth) + " " + unit);
            if (this.overlay.getIndex("positive polarity") != -1) {
                pol = this.overlay.get(this.overlay.getIndex("positive polarity")).getPolygon();
                outf.println("POSLOC " + IJ.d2s(pol.xpoints[0], 0) + ", " + IJ.d2s(pol.ypoints[0], 0));
            }
            outf.println("PATH");
            pol = this.overlay.get(this.overlay.getIndex("path")).getPolygon();
            for (i = 0; i < pol.npoints; i++) {
                outf.println("  " + IJ.d2s(pol.xpoints[i], 0) + ", "+ IJ.d2s(pol.ypoints[i], 0));
            }
            outf.println("END");
            for (n = 0; n < this.overlay.size(); n++) {
                if (this.overlay.get(n).getName().equals("hole")) {
                    outf.println("HOLE");
                    pol = this.overlay.get(n).getPolygon();
                    for (i = 0; i < pol.npoints; i++)
                        outf.println("  " + IJ.d2s(pol.xpoints[i], 0) + ", "+ IJ.d2s(pol.ypoints[i], 0));
                    outf.println("END");
                }
            }
            if (this.overlay.getIndex("points") != -1) {
                outf.println("POINTS");
                pol = this.overlay.get(this.overlay.getIndex("points")).getPolygon();
                for (i = 0; i < pol.npoints; i++) {
                    outf.println("  " + IJ.d2s(pol.xpoints[i], 0) + ", " + IJ.d2s(pol.ypoints[i], 0));
                }
                outf.println("END");
            }
            if (this.overlay.getIndex("randon points") != -1) {
                outf.println("RANDOM_POINTS");
                pol = this.overlay.get(this.overlay.getIndex("randon points")).getPolygon();
                for (i = 0; i < pol.npoints; i++) {
                    outf.println("  " + IJ.d2s(pol.xpoints[i], 0) + ", " + IJ.d2s(pol.ypoints[i], 0));
                }
                outf.println("END");
            }
            outf.close();
        } catch (Exception e) {
            return false;
        }
        writeIDtext(imp);
        drawComponents(imp);
        this.ntot++;
        SaveDialog sd = new SaveDialog("Save analyzed image",
                imp.getShortTitle(),
                ".a.tif");
        if (sd.getFileName() != null) {
            FileSaver saveTiff = new FileSaver(imp);
            saveTiff.saveAsTiff(sd.getDirectory() + sd.getFileName());
        }
        return true;
    }


    private void drawComponents(ImagePlus imp) {
        Polygon pol;
        int n, x, y;

        for (n = 0; n < this.overlay.size(); n++) {
            imp.setColor(this.overlay.get(n).getStrokeColor());
            if (this.overlay.get(n).getName().equals("points")) {
                pol = this.overlay.get(n).getPolygon();
                for (i = 0; i < pol.npoints; i++) {
                    x = pol.xpoints[i];
                    y = pol.ypoints[i];
                    imp.getProcessor().drawLine(x - 3, y, x + 3, y);
                    imp.getProcessor().drawLine(x, y - 3, x, y + 3);
                }
            }  else {
                this.overlay.get(n).drawPixels(imp.getProcessor());
            }
        }
    }


    private Point findxy(Polygon pol, ImagePlus imp) {
        int miny, x;

        miny = imp.getHeight();
        x = imp.getWidth();
        for (i = 0; i < pol.npoints; i++) {
            if (pol.ypoints[i] < miny) {
                miny = pol.ypoints[i];
                x = pol.xpoints[i];
                if (pol.ypoints[i] == miny && pol.xpoints[i] < x) {
                    x = pol.xpoints[i];
                }
            }
        }
        return new Point(x, miny);
    }


    private void writeIDtext(ImagePlus imp) {
        TextRoi profileLabel;
        Polygon pol;
        Point p;
        int locx, locy, size;
        String label = "";

        for (i = 0; i < this.ID.length(); i++) {
            label += this.ID.charAt(i);
        }
        size = imp.getHeight() / 42;  // adjust font size for image size (by an arbitrary factor)
        TextRoi.setFont(TextRoi.getFont(), size, Font.BOLD);
        profileLabel = new TextRoi(0, 0, label);
        profileLabel.setAntialiased(true);
        pol = this.overlay.get(this.overlay.getIndex("path")).getPolygon();
        p = findxy(pol, imp);
        locy = p.y - profileLabel.getBounds().height;
        locx = p.x - profileLabel.getBounds().width;
        if (locx < 0) locx = 3;
        if (locy < 0) locy = 3;
        profileLabel.setLocation(locx, locy);
        imp.setColor(textCol);
        profileLabel.drawPixels(imp.getProcessor());
        imp.setColor(Color.black);
    }


    public void clear() {
        this.dirty = false;
        this.overlay.clear();
        this.comment = "";
        this.ID = "";
    }


    /**
     * Main method for debugging.
     *
     * For debugging, it is convenient to have a method that starts ImageJ, loads an
     * image and calls the plugin, e.g. after setting breakpoints.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        // set the plugins.dir property to make the plugin appear in the Plugins menu
        Class<?> cl = DistToPath_.class;
        String url = cl.getResource("/" + cl.getName().replace('.', '/') + ".class").toString();
        String pluginsDir = url.substring(5, url.length() - cl.getName().length() - 6);
        System.setProperty("plugins.dir", pluginsDir);

        // start ImageJ
        new ImageJ();

        ImagePlus image = IJ.openImage("");
        image.show();

        // run the plugin
        IJ.runPlugIn(cl.getName(), "");
    }

} // end of DistToPath
