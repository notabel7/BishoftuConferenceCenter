package com.conferenceCenter.util;

import java.awt.*;

/**
 * Central repository for all UI styling — colours, fonts, dimensions.
 */
public final class UIConstants {

    private UIConstants() {}

    // ── Colours ───────────────────────────────────────────────────────────
    public static final Color PRIMARY        = new Color(26,  35,  126); // deep blue
    public static final Color SECONDARY      = new Color(249, 168,  37); // gold
    public static final Color BACKGROUND     = new Color(240, 242, 250);
    public static final Color PANEL_BG       = Color.WHITE;
    public static final Color HEADER_FG      = Color.WHITE;
    public static final Color TABLE_ALT_ROW  = new Color(232, 234, 246);
    public static final Color DANGER         = new Color(183,  28,  28);
    public static final Color SUCCESS        = new Color( 27,  94,  32);
    public static final Color BORDER_COLOR   = new Color(189, 193, 226);
    public static final Color INPUT_BG       = new Color(250, 250, 255);
    public static final Color SELECTED_ROW   = new Color(197, 202, 233);

    // ── Fonts ─────────────────────────────────────────────────────────────
    public static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD,  26);
    public static final Font FONT_H2     = new Font("Segoe UI", Font.BOLD,  16);
    public static final Font FONT_BODY   = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD   = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font FONT_TABLE  = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD,  13);

    // ── Sizes ─────────────────────────────────────────────────────────────
    public static final Dimension BTN_SIZE    = new Dimension(130, 34);
    public static final Dimension BTN_WIDE    = new Dimension(160, 34);
    public static final int       FIELD_H     = 32;
    public static final int       ROW_HEIGHT  = 28;

    // ── DPI scale detection ───────────────────────────────────────────────
    /**
     * Physical pixels per logical pixel on the primary display.
     * e.g. 1.0 at 100%, 1.25 at 125%, 1.5 at 150%, 2.0 at 200%.
     * Detected once at class-load time; stored for all icon renders.
     */
    private static final double SCALE = detectScale();

    private static double detectScale() {
        try {
            java.awt.geom.AffineTransform tx = java.awt.GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .getDefaultTransform();
            return Math.max(1.0, tx.getScaleX());
        } catch (Throwable t) { return 1.0; }
    }

    // ── HiDPI-aware icon wrapper ──────────────────────────────────────────
    /**
     * Wraps a BufferedImage rendered at PHYSICAL pixel dimensions, but
     * reports LOGICAL dimensions (w, h) to Swing's layout engine.
     *
     * Why this works: on HiDPI displays Java's Swing pipeline applies a
     * scale transform to the Graphics2D before calling paintIcon().  When
     * we draw getImage() at logical size (lw × lh), the transform maps
     * those logical units to exactly pw × ph physical pixels — matching
     * our pre-rendered image 1-for-1 → perfectly sharp, zero stretch.
     */
    private static final class HiDpiIcon extends javax.swing.ImageIcon {
        private final int lw, lh;   // logical (layout) dimensions

        HiDpiIcon(java.awt.image.BufferedImage img, int lw, int lh) {
            super(img);
            this.lw = lw; this.lh = lh;
        }

        @Override public int getIconWidth()  { return lw; }
        @Override public int getIconHeight() { return lh; }

        @Override
        public synchronized void paintIcon(java.awt.Component c,
                                           java.awt.Graphics g, int x, int y) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                                java.awt.RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            // Draw physical image at logical size; scaled Graphics2D maps
            // logical → physical automatically, so source pixels line up
            // 1-to-1 with screen pixels → crisp at any DPI setting.
            g2.drawImage(getImage(), x, y, lw, lh, null);
            g2.dispose();
        }
    }

    // ── Icon loaders ──────────────────────────────────────────────────────

    /**
     * Loads /icons/<name> at 18×18 logical px (physical size auto-scaled).
     * Tries SVG first via SVGSalamander, falls back to PNG.
     * Returns null silently if neither is found.
     */
    public static javax.swing.ImageIcon loadIcon(String name) {
        return loadIcon(name, 18, 18);
    }

    /**
     * Loads /icons/<name> at w×h logical pixels.
     * Renders at w*SCALE × h*SCALE physical pixels for HiDPI sharpness.
     */
    public static javax.swing.ImageIcon loadIcon(String name, int w, int h) {
        int pw = (int) Math.ceil(w * SCALE);   // physical width
        int ph = (int) Math.ceil(h * SCALE);   // physical height

        // Try SVG first
        String svgName = name.replaceAll("\\.[^.]+$", "") + ".svg";
        java.net.URL svgUrl = UIConstants.class.getResource("/icons/" + svgName);
        if (svgUrl != null) {
            java.awt.image.BufferedImage img = renderSVG(svgUrl, pw, ph);
            if (img != null) return new HiDpiIcon(img, w, h);
        }
        // Fall back to PNG
        try {
            java.net.URL url = UIConstants.class.getResource("/icons/" + name);
            if (url != null) {
                java.awt.image.BufferedImage bi = scalePNG(url, pw, ph);
                if (bi != null) return new HiDpiIcon(bi, w, h);
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Same as loadIcon(18×18) but tints every non-transparent pixel white.
     * Used for sidebar icons on the dark-blue background.
     * Renders at physical pixel size for HiDPI sharpness, then tints.
     */
    public static javax.swing.ImageIcon loadIconWhite(String name) {
        int pw = (int) Math.ceil(18 * SCALE);
        int ph = (int) Math.ceil(18 * SCALE);

        // Render source at physical size — SVG preferred, PNG fallback
        java.awt.image.BufferedImage src = null;
        String svgName = name.replaceAll("\\.[^.]+$", "") + ".svg";
        java.net.URL svgUrl = UIConstants.class.getResource("/icons/" + svgName);
        if (svgUrl != null) src = renderSVG(svgUrl, pw, ph);

        if (src == null) {
            try {
                java.net.URL url = UIConstants.class.getResource("/icons/" + name);
                if (url != null) src = scalePNG(url, pw, ph);
            } catch (Exception ignored) {}
        }
        if (src == null) return null;

        // Tint: replace every visible pixel with white, preserve alpha
        java.awt.image.BufferedImage out =
            new java.awt.image.BufferedImage(pw, ph,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < pw; x++) {
            for (int y = 0; y < ph; y++) {
                int argb  = src.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha > 10) out.setRGB(x, y, (alpha << 24) | 0x00FFFFFF);
            }
        }
        return new HiDpiIcon(out, 18, 18);
    }

    // ── Private rendering helpers ─────────────────────────────────────────

    /**
     * Renders an SVG to a crisp ARGB BufferedImage at exactly pw×ph
     * physical pixels using SVGSalamander with full antialiasing.
     * Returns null on any failure (missing JAR, bad SVG, etc.).
     */
    private static java.awt.image.BufferedImage renderSVG(
            java.net.URL url, int pw, int ph) {
        try {
            com.kitfox.svg.app.beans.SVGIcon icon =
                new com.kitfox.svg.app.beans.SVGIcon();
            icon.setSvgURI(url.toURI());
            icon.setScaleToFit(true);
            icon.setAntiAlias(true);
            icon.setPreferredSize(new java.awt.Dimension(pw, ph));

            java.awt.image.BufferedImage img =
                new java.awt.image.BufferedImage(pw, ph,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                java.awt.RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(java.awt.RenderingHints.KEY_STROKE_CONTROL,
                java.awt.RenderingHints.VALUE_STROKE_PURE);
            icon.paintIcon(null, g2, 0, 0);
            g2.dispose();
            return img;
        } catch (Throwable t) { return null; }
    }

    /**
     * Loads a PNG from a URL and scales it to pw×ph using high-quality
     * bilinear interpolation into an ARGB BufferedImage.
     */
    private static java.awt.image.BufferedImage scalePNG(
            java.net.URL url, int pw, int ph) {
        try {
            javax.swing.ImageIcon raw = new javax.swing.ImageIcon(url);
            java.awt.image.BufferedImage bi =
                new java.awt.image.BufferedImage(pw, ph,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2 = bi.createGraphics();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                java.awt.RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(
                raw.getImage().getScaledInstance(pw, ph, java.awt.Image.SCALE_SMOOTH),
                0, 0, null);
            g2.dispose();
            return bi;
        } catch (Exception e) { return null; }
    }
}
