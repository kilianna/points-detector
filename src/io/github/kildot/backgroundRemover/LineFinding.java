package io.github.kildot.backgroundRemover;

import java.util.Arrays;
import java.util.Collections;

public class LineFinding {

    private static class Matrix {

        public double ux;
        public double vx;
        public double uy;
        public double vy;
        public double x;
        public double y;

        public Matrix(double ux, double vx, double uy, double vy, double x, double y) {
            this.ux = ux;
            this.vx = vx;
            this.uy = uy;
            this.vy = vy;
            this.x = x;
            this.y = y;
        }

        public Matrix copy() {
            return new Matrix(ux, vx, uy, vy, x, y);
        }

        public void absoluteScaleX(double s) {
            double mul = s / Math.sqrt(ux * ux + vx * vx);
            ux *= mul;
            vx *= mul;
        }

        public void invert() {
            double idet = 1.0 / (ux * vy - uy * vx);
            double _x = x;
            double _ux = ux;
            x = (uy * y - vy * x) * idet;
            y = (vx * _x - ux * y) * idet;
            ux = vy * idet;
            vy = _ux * idet;
            vx = -vx * idet;
            uy = -uy * idet;
        }

        public void mul(Point p, Point output) {
            output.x = x + p.x * ux + p.y * uy;
            output.y = y + p.x * vx + p.y * vy;
        }

        public void mulInPlace(Point p) {
            double px = p.x;
            p.x = x + px * ux + p.y * uy;
            p.y = y + px * vx + p.y * vy;
        }

    }

    private static class MatrixSimplified {

        public double u;
        public double v;
        public double x;
        public double y;

        public MatrixSimplified(double u, double v, double x, double y) {
            this.u = u;
            this.v = v;
            this.x = x;
            this.y = y;
        }

        public MatrixSimplified copy() {
            return new MatrixSimplified(u, v, x, y);
        }

        public void absoluteScale(double s) {
            double mul = s / Math.sqrt(u * u + v * v);
            u *= mul;
            v *= mul;
        }

        public void invert() {
            double idet = 1.0 / (u * u + v * v);
            double xx = x;
            x = -(u * x + v * y) * idet;
            y = (v * xx - u * y) * idet;
            u = u * idet;
            v = -v * idet;
        }

        public void mulInPlace(Point p) {
            double px = p.x;
            p.x = x + u * p.x - v * p.y;
            p.y = y + v * px + u * p.y;
        }

        public Point mul(Point p) {
            double rx = x + u * p.x - v * p.y;
            double ry = y + v * p.x + u * p.y;
            return new Point(rx, ry);
        }

        public double mulGetY(Point p) {
            return y + v * p.x + u * p.y;
        }

        public void mul(Point p, Point output) {
            output.x = x + u * p.x - v * p.y;
            output.y = y + v * p.x + u * p.y;
        }
    }

    public static class Point {

        public double x;
        public double y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private Point[] sortPoints(Point[] points, boolean rosnaco) {
        Point[] sorted = Arrays.copyOf(points, points.length);
        if (rosnaco) {
            Arrays.sort(sorted, (Point a, Point b) -> Double.compare(a.x, b.x));
        } else {
            Arrays.sort(sorted, (Point a, Point b) -> -Double.compare(a.x, b.x));
        }
        return sorted;
    }

    private Point[] policzObwiednie(Point[] points, boolean isNoise) {
        if (points.length <= 2) {
            return sortPoints(points, true);
        }
        // Jeżeli jest to szum wykonaj rotację 180st.
        // Posortuj punkty po x rosnąco
        Point[] pointsOriginal = points;
        points = sortPoints(points, !isNoise);
        if (isNoise) {
            for (int i = 0; i < points.length; i++) {
                points[i].x = -points[i].x;
                points[i].y = -points[i].y;
            }
        }
        // Wyznacz punkt, który ma A: min y i B: max x ze wszystkich punków
        int indexA = 0; // min y
        int indexB = points.length - 1; // max x
        for (int i = 1; i < points.length; i++) {
            if (points[i].y < points[indexA].y) {
                indexA = i;
            }
        }
        if (indexA == indexB) {
            return new Point[]{points[indexA]};
        }
        if (indexA == indexB - 1) {
            return new Point[]{points[indexA], points[indexB]};
        }
        int liczbaMiedzy = policzObwiednieMiedzy(points, indexA, indexB);
        points[indexA + liczbaMiedzy + 1] = points[indexB];
        Point[] result = Arrays.copyOfRange(points, indexA, indexA + liczbaMiedzy + 2);
        if (isNoise) {
            for (int i = 0; i < pointsOriginal.length; i++) {
                pointsOriginal[i].x = -pointsOriginal[i].x;
                pointsOriginal[i].y = -pointsOriginal[i].y;
            }
            Collections.reverse(Arrays.asList(result));
        }
        return result;
    }

    private int policzObwiednieMiedzy(Point[] points, int indexA, int indexB) {
        // Wyjdź natychmiast, jeżeli nie ma nic więcej pomiędzy punktami.
        if (indexA == indexB - 1) {
            return 0;
        }
        // Utwórz macierz przekształcającą odcinek AB na odcinek (0,0) (1,0).
        Point a = points[indexA];
        Point b = points[indexB];
        MatrixSimplified tr = new MatrixSimplified(b.x - a.x, b.y - a.y, a.x, a.y);
        tr.invert();
        // Pozostaw tylko to co poniżej prostej AB (poniżej osi X po przekształceniu)
        // i wybierz najbardziej wysunięty punkt D.
        int resultOffset = indexA + 1;
        double minDy = 1.0;
        int indexD = -1;
        for (int i = indexA + 1; i < indexB; i++) {
            Point p = points[i];
            double trpy = tr.mulGetY(p);
            if (trpy >= 0.0) {
                continue;
            }
            if (trpy < minDy) {
                indexD = resultOffset;
                minDy = trpy;
            }
            points[resultOffset] = p;
            resultOffset++;
        }
        // Dodaj ewentualny duplikat punktu B na koniec.
        points[resultOffset] = points[indexB];
        int indexBDuplikat = resultOffset;
        // Jeżeli nie da się znaleźć punktu D, więc odcinek AB należy do obwiedni.
        if (indexD < 0) {
            return 0;
        }
        // Policz obwiednie w dwuch zakresach: AD i DB
        int miedzyAD = policzObwiednieMiedzy(points, indexA, indexD);
        int miedzyDB = policzObwiednieMiedzy(points, indexD, indexBDuplikat);
        // Przesuń elementy tablicy, jeżeli powyższe liczenie obwiedni pozostawiło dzióry
        System.arraycopy(points, indexD, points, indexA + miedzyAD + 1, miedzyDB + 1);
        // Zwróć liczbę elementów między zadanymi punktami A i B
        return miedzyAD + 1 + miedzyDB;
    }

    private double[] wyznaczOdcinekMiedzy(Point[] obwiednia1, Point[] obwiednia2, double weight) {
        Point[] naj = null;
        Point najKier = null;
        double najWeight = 0.0;
        double najDlugosc = Double.MAX_VALUE;
        for (double cymul = -1.0; cymul < 1.1; cymul += 2.0) {
            Point c = new Point(0, 0);
            int punkt = 0;
            for (int i = 0; i < obwiednia1.length - 1; i++) {
                Point a = obwiednia1[i];
                Point b = obwiednia1[i + 1];
                Matrix unshift = new Matrix(b.x - a.x, b.y - a.y, 0.0, 1.0, a.x, a.y);
                unshift.absoluteScaleX(1.0);
                Matrix shift = unshift.copy();
                shift.invert();
                while (punkt < obwiednia2.length && obwiednia2[punkt].x < a.x) {
                    punkt++;
                }
                while (punkt < obwiednia2.length && obwiednia2[punkt].x <= b.x) {
                    shift.mul(obwiednia2[punkt], c);
                    double dlugosc = c.y * cymul;
                    if (dlugosc < najDlugosc) {
                        najDlugosc = dlugosc;
                        c.y = 0;
                        unshift.mulInPlace(c);
                        naj = new Point[]{
                            new Point(obwiednia2[punkt].x, obwiednia2[punkt].y),
                            new Point(c.x, c.y),};
                        najKier = new Point(b.x - a.x, b.y - a.y);
                        najWeight = weight;
                    }
                    punkt++;
                }
            }
            Point[] tmp = obwiednia1;
            obwiednia1 = obwiednia2;
            obwiednia2 = tmp;
            weight = 1.0 - weight;
        }
        if (naj == null) {
            return null;
        }
        return parametryProstej(naj, najKier, najWeight);
    }

    private double[] parametryProstej(Point[] odcinek, Point kierunek, double weight) {
        Matrix shift = new Matrix(kierunek.x, kierunek.y, 0.0, 1.0, 0, 0);
        shift.absoluteScaleX(1.0);
        shift.invert();
        Point mid = new Point(weight * odcinek[0].x + (1.0 - weight) * odcinek[1].x, weight * odcinek[0].y + (1.0 - weight) * odcinek[1].y);
        shift.mulInPlace(mid);
        double a = kierunek.y / kierunek.x;
        double b = mid.y;
        return new double[]{a, b};
    }

    public double[] calc(Point[] points, Point[] noise, double weight) {
        Point[] obwiedniaPunktow = policzObwiednie(points, false);
        Point[] obwiedniaSzumu = policzObwiednie(noise, true);
        return wyznaczOdcinekMiedzy(obwiedniaPunktow, obwiedniaSzumu, weight);
    }

}
