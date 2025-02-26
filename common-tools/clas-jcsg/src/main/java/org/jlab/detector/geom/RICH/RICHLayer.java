package org.jlab.detector.geom.RICH;

import java.util.ArrayList;
import java.util.List;
import org.jlab.geom.prim.Face3D;
import org.jlab.geom.prim.Shape3D;
import org.jlab.geom.prim.Sphere3D;
import org.jlab.geom.prim.Triangle3D;
import org.jlab.geom.prim.Point3D;
import org.jlab.geom.prim.Line3D;
import org.jlab.geom.prim.Vector3D;
import org.jlab.geom.prim.Plane3D;

import eu.mihosoft.vrl.v3d.Vertex;
import eu.mihosoft.vrl.v3d.Vector3d;
import eu.mihosoft.vrl.v3d.Polygon;

/**
* @author mcontalb
* A layer in the RICH consists of an array of components
*/
public class RICHLayer extends ArrayList<RICHComponent> {

    private static final double RAD = RICHGeoConstants.RAD;
    private RICHGeoParameters  geopar;

    private int id;         // layer id in RICH reconstruction
    private int idgea;      // layer id in RICHGeant4Factory and CCDB database
    private int type;       // layer type (1=aerogel, 2=front mirror, 3=planar mirror, 4=spherical mirror, 5=mapmt)
    private int sector;     // sector 
    private String name;    // layer name
    private String vers;    // layer orientation label

    private Vector3D vinside = new Vector3D(0., 0., 0.);    // layer orientation (normal versor)

    private Vector3D barycenter           = null;
    private Shape3D  global_surf          = null;
    private Shape3D  tracking_surf        = null;
    private Sphere3D tracking_sphere      = null;
    private Shape3D  nominal_plane        = null;

    private ArrayList<Integer> compo_list          = new ArrayList<Integer>();
    
    private RICHFrame local_frame = new RICHFrame();
    private RICHPixel pmtpixels   = null;


    // ----------------
    public RICHLayer(int isec, int ilay, RICHGeoParameters geopar) {
    // ----------------

        this.id     = ilay;
        this.idgea  = RICHLayerType.get_Type(ilay).ccdb_ila();
        this.type   = RICHLayerType.get_Type(ilay).type();
        this.name   = RICHLayerType.get_Type(ilay).name();
        this.vers   = RICHLayerType.get_Type(ilay).vers();
        this.sector = isec;

        this.geopar = geopar;

        if(vers.equals("front")) vinside = RICHGeoConstants.vfront.asUnit();
        if(vers.equals("left")) vinside = RICHGeoConstants.vleft.asUnit();
        if(vers.equals("right")) vinside = RICHGeoConstants.vright.asUnit();
        if(vers.equals("bottom")) vinside = RICHGeoConstants.vbottom.asUnit();
        if(vers.equals("sphere")) vinside = RICHGeoConstants.vsphere.asUnit();
        if(vers.equals("back")) vinside = RICHGeoConstants.vback.asUnit();

    }


    // ----------------
    public int id() { return this.id; }
    // ----------------

    // ----------------
    public int idgea() { return this.idgea; }
    // ----------------

    // ----------------
    public int type() { return this.type; }
    // ----------------

    // ----------------
    public int sector() { return this.sector; }
    // ----------------

    // ----------------
    public String name() { return this.name; }
    // ----------------

    // ----------------
    public Vector3D get_Vinside() { return vinside; }
    // ----------------

    // ----------------
    public void set_Vinside(Vector3D vers) { this.vinside = vers; }
    // ----------------

    // ----------------
    public int get_size() { return this.size(); }
    // ----------------

    // ----------------
    public void set_PMTPixels(RICHPixel pmtpixels){ this.pmtpixels = pmtpixels;}
    // ----------------
  
    // ----------------
    public RICHPixel get_PMTPixels(){ return this.pmtpixels;}
    // ----------------
  
    // ----------------
    public Shape3D get_TrackingSurf() { 
    // ----------------
        return get_TrackingSurf(-1); 
    }
  
    // ----------------
    public Shape3D get_TrackingSurf(int ico) { 
    // ----------------
        if(ico<0 || ico>=this.size()) return this.tracking_surf;
        return get(ico).get_TrackingSurf();
    }
  
    // ----------------
    public void set_TrackingSurf(Shape3D plane) { tracking_surf=plane;}
    // ----------------
  
    // ----------------
    public void set_TrackingSurf(Shape3D plane, int ico) { get(ico).set_TrackingSurf(plane);}
    // ----------------
  
    // ----------------
    public ArrayList<Integer> get_CompoList() { return compo_list; }
    // ----------------
  
    // ----------------
    public void set_CompoList(ArrayList<Integer> list) { compo_list = list; }
    // ----------------
  
    // ----------------
    public int get_CompoIndex(int ifa) { 
    // ----------------
        if(ifa>=0 && ifa<compo_list.size()) return compo_list.get(ifa); 
        return -1;
    }

    // ----------------
    public Face3D get_Face(int ifa) { 
    // ----------------
        return get_CompoFace(-1, ifa);
    }

    // ----------------
    public Face3D get_CompoFace(int icompo, int ifa) { 
    // ----------------
        
        if(icompo<0 || icompo>=this.size()) return global_surf.face(ifa);
        return get_TrackingSurf(icompo).face(ifa);
    }

    // ----------------
    public Vector3D get_LayerNormal() { return get_CompoNormal(-1, vinside); }
    // ----------------

    // ----------------
    public Vector3D get_LayerNormal(Vector3D orientation) { return get_CompoNormal(-1, orientation); }
    // ----------------

    // ----------------
    public Vector3D get_CompoNormal(int icompo) { return get_CompoNormal(icompo, vinside); }
    // ----------------

    // ----------------
    public Vector3D get_CompoNormal(int icompo, Vector3D orientation) { 
    // ----------------

        if(icompo<0 || icompo>=this.size()){
            /*
            * Any face of global plane with given orientation
            */
            Shape3D shape = get_GlobalSurf();
            for(int ifa=0; ifa<shape.size(); ifa++){
                Vector3D normal = toTriangle3D(shape.face(ifa)).normal();
                if(normal.dot(orientation)>0) {
                    return normal.asUnit();
                }
            }
            return new Vector3D(0.0, 0.0, 0.0); 

        }else{
        
            /*
            * Any of the component faces in the component Surf with given orientation 
            */
            Shape3D shape = get_TrackingSurf(icompo);
            for(int ifa=0; ifa<shape.size(); ifa++){
                Vector3D normal = toTriangle3D(shape.face(ifa)).normal();
                if(normal.dot(orientation)>0) {
                    return normal.asUnit();
                }
            }
        return new Vector3D(0.0, 0.0, 0.0); 

        }

    }

    // ----------------
    public Vector3D get_FaceNormal(int icompo, int ifa) { 
    // ----------------

        /*
        * Normal to the given face for given component
        */
        Vector3D normal = new Vector3D();

        if(icompo<0 || icompo>=this.size()){
            if(ifa<0 || ifa>=global_surf.size())return normal;
            normal = toTriangle3D(global_surf.face(ifa)).normal();
        }else{
            if(ifa<0 || ifa>=tracking_surf.size())return normal;
            normal = toTriangle3D(tracking_surf.face(ifa)).normal();
        }
         
        return normal.asUnit();

    }

    // ----------------
    public void set_GlobalSurf(Shape3D plane) { global_surf = plane; }
    // ----------------
  
    // ----------------
    public Shape3D get_GlobalSurf() { return global_surf; }
    // ----------------

    // ----------------
    public void set_TrackingSphere(Sphere3D sphere) { tracking_sphere = sphere; }
    // ----------------

    // ----------------
    public void set_TrackingSphere(Sphere3D sphere, int ico) { this.get(ico).set_TrackingSphere(sphere); }
    // ----------------
  
    // ----------------
    public Sphere3D get_TrackingSphere() { return get_TrackingSphere(-1); }
    // ----------------
  
    // ----------------
    public Sphere3D get_TrackingSphere(int ico) { 
    // ----------------
        if(ico<0 || ico>=this.size()) return this.tracking_sphere;
        return this.get(ico).get_TrackingSphere(); }

    // ----------------
    public void set_NominalPlane(Shape3D plane) { nominal_plane = plane; }
    // ----------------
  
    // ----------------
    public Shape3D get_NominalPlane() { return nominal_plane; }
    // ----------------
  
    //------------------------------
    public Vector3D get_LayerCSGBary(){ return get_CompoCSGBary(-1); }
    //------------------------------

    //------------------------------
    public Plane3D get_TrajPlane(){ 
    //------------------------------

        int debugMode = 0;

        double toIP = -1.;
        if(name.equals("mapmts")) toIP=1.;

        Point3D pos =  get_SurfBary(-1, vinside.multiply(toIP)).toPoint3D() ;
        Vector3D ver = get_LayerNormal(vinside.multiply(toIP) );
        Plane3D plane = new Plane3D(pos, ver);

        if(debugMode>=1) { 
            System.out.format("get_TrajPlane %d %s to IP %7.2f %s\n",id,name,toIP,vinside.multiply(toIP).toStringBrief(2));
            System.out.format("get_TrajPlane %s %s \n",pos.toStringBrief(2), ver.toStringBrief(2));
            plane.show();
        }
        return plane;

    }

    //------------------------------
    public Vector3D get_CompoBary(int icompo){ 
    //------------------------------

        int debugMode = 0;

        Vector3D bary  = get_SurfBary(icompo, vinside); 
        Vector3D sbary = get_SurfBary(icompo, vinside.multiply(-1.));

        if(debugMode>0)System.out.format("%s %s \n",bary.toStringBrief(2), sbary.toStringBrief(2));
        return (bary.add(sbary)).multiply(0.5);

    }


    //------------------------------
    public Vector3D get_CompoCSGBary(int icompo){
    //------------------------------

        int debugMode = 0;

        List<Point3D> pts = new ArrayList<Point3D>();
        Vector3D bary = new Vector3D(0., 0., 0.);
        double nb=0.0;

        int icomi=icompo;
        int icoma=icompo+1;
        if(icompo<0 || icompo>=this.size()){
            icomi=0;
            icoma=this.size();
        }
        if(debugMode>=1)System.out.format(" Generate bary for lay %3d and compos %4d:%4d\n", id, icomi, icoma);

        for (int ico=icomi; ico<icoma; ico++){

            RICHComponent compo = this.get(ico);

            for (Polygon pol: compo.get_CSGVol().getPolygons()){
                for (Vertex ver: pol.vertices){

                    Point3D p = toPoint3D(ver);
                    int found = 0;
                    for(int i=0; i<pts.size(); i++){
                        if(p.distance(pts.get(i))<1.e-3)found=1;
                    }

                    if(found==0){
                        pts.add(p);
                        if(debugMode>=1)System.out.format(" bary from vertex: %s\n", p.toStringBrief(2));
                        bary.add(p.toVector3D());
                        nb++;
                    }
               }
            }
        }

        if(nb==0.0)nb=1.0;
        if(debugMode>=1)System.out.format(" got bary: %s\n", bary.multiply(1/nb).toStringBrief(2));
        return bary.multiply(1/nb);

    }


    //------------------------------
    public Vector3D get_CompoCenter(int icompo, Vector3D vers){
    //------------------------------

        /*
        *  Center of the component sphere 
        *  vers is used to select the right faces (for aerogel)
        */
        int debugMode = 0;

        Vector3D arm = vers.multiply(get(icompo).get_radius());
        return get_SurfBary(icompo, vers).add(arm);

    }

    //------------------------------
    public ArrayList<Point3D> select_Vertexes(Shape3D surf, Vector3D vers) {
    //------------------------------

        int debugMode = 0;

        ArrayList<Point3D> pts = new ArrayList<Point3D>();
        for(int ifa=0; ifa<surf.size(); ifa++){

            Face3D f = surf.face(ifa);
            for (int ipo=0; ipo<3; ipo++){
                Point3D p = f.point(ipo);
                if(debugMode>=1)System.out.format("Vertex %s \n", p.toStringBrief(2));
            }

            if(toTriangle3D(f).normal().dot(vers)<0)continue;
            for (int ipo=0; ipo<3; ipo++){

                Point3D p = f.point(ipo);
                int found = 0;
                for(int i=0; i<pts.size(); i++){
                    if(p.distance(pts.get(i))<1.e-3)found=1;
                }

                if(found==0){
                    pts.add(p);
                    if(debugMode>=1)System.out.format(" --> New Vertex %s \n", p.toStringBrief(2), pts.size());
                }else{
                    if(debugMode>=1)System.out.format(" --> Old Vertex %s \n",p.toStringBrief(2));
                }

            }
        }

        return pts;

    }

    //------------------------------
    public Vector3D get_SurfBary(){ return get_SurfBary(-1, vinside); }
    //---------------------------- --

    //------------------------------
    public Vector3D get_SurfBary(Vector3D vers){ return get_SurfBary(-1, vers); }
    //---------------------------- --

    //------------------------------
    public Vector3D get_SurfBary(int icompo, Vector3D vers){
    //------------------------------

        /*
        *  Center of the component plane without double counting of vertexes 
        *  vers is used to select the right faces (for aerogel)
        */
        int debugMode = 0;

        if(debugMode>=1)System.out.format(" Get surf bary for compo %d vers %s\n", icompo,vers.toStringBrief(2));

        Shape3D surf = null;
        surf=get_TrackingSurf(icompo);
        if (surf==null) surf=get_GlobalSurf();
        if (surf==null) return null;

        Vector3D bary = new Vector3D(0., 0., 0.);
        double np=0.0;
        List<Point3D> pts = select_Vertexes(surf, vers);
        for(Point3D p: pts){
            bary.add(p.toVector3D());
            np += 1;
        }

        if(np==0.0)np=1.0;  
        bary.scale(1/np);
        
        if(!this.is_spherical_mirror())return bary;

        Sphere3D sphere = null;
        sphere=get_TrackingSphere(icompo);
        if (sphere==null) return null;

        /*
        * Exrapolate the point at the curved surface
        */
        Point3D pary = bary.toPoint3D();
        Line3D ray = new Line3D( sphere.getCenter(), pary);
        if(debugMode>=1)System.out.format(" Line3D %s \n",ray.toString());

        List<Point3D> crosses = new ArrayList<Point3D>();
        int ncross = sphere.intersection(ray, crosses);
        for (int ic=0; ic<ncross; ic++){
            Point3D new_point = crosses.get(ic);
            if(debugMode>=1)System.out.format(" cross with sphere %s --> %7.2f \n",new_point.toStringBrief(2),new_point.distance(pary));
            if(new_point.distance(pary)<geopar.MAX_SPHE_DIST){
                 
                return new_point.toVector3D();

            }
         }

         return null;
    }


    //------------------------------
    public int get_TileQuadrant(int Nqua, int icompo, Point3D point, ArrayList<Point3D> verts) {
    //------------------------------

        int debugMode = 0;

        Shape3D surf = get_TrackingSurf(icompo);
        Vector3D vers = new Vector3D(vinside.multiply(-1.));
        Point3D surfb = toPoint3D(get_SurfBary(icompo, vers));

        if(debugMode==1)System.out.format(" --- N quadrant --- %d \n",Nqua);
        for(int i=0; i<verts.size(); i++){
            if(debugMode==1)System.out.format(" vtx %s \n",verts.get(i).toStringBrief(2));
        }
        if(debugMode==1)System.out.format(" Layer id %d\n",id);

        Vector3D vbary   = new Vector3D(0.,0.,0.);
        Vector3D  stpx   = new Vector3D(0.,0.,0.);
        Vector3D  stpy   = new Vector3D(0.,0.,0.);
        Vector3D  vcross = new Vector3D(0.,0.,0.);
        Point3D   v0     = new Point3D(0.,0.,0.);

        double aero_dim = RICHGeoConstants.AERO_REF_DIMENSION*RICHGeoConstants.CM;
        double aero_sca = 1.;
        // first two tiles are shorter
        if(id==RICHLayerType.AEROGEL_2CM_B1.id() && (icompo==0 || icompo==1)) {
            aero_dim=RICHGeoConstants.AERO_CUT_DIMENSION*RICHGeoConstants.CM;  
            aero_sca = aero_dim/aero_sca;
        }
 
        int found = 0;
        for(int i=0; i<verts.size(); i++){
            for(int j=i+1; j<verts.size(); j++){

                double dist = verts.get(i).distance(verts.get(j));
                double disty = Math.abs(verts.get(i).y()-verts.get(j).y());
                if(debugMode==1)System.out.format(" vtx %3d %3d dist %7.2f aero_sca %7.2f\n",i,j, dist,aero_sca);
                if(found==0 && Math.abs(dist - aero_dim)<0.2 && disty<10) {

                    Point3D mid = verts.get(i).midpoint(verts.get(j));

                    v0 = verts.get(i);
                    Vector3D vb = v0.vectorTo(surfb);
                    stpx = v0.vectorTo(mid);
                    stpy = v0.vectorTo(mid);

                    vcross = stpx.cross(vb);
                    vcross.rotate(stpy, 90./RAD);
                    stpy.scale(aero_sca);

                    vbary = verts.get(i).toVector3D();
                    vbary.add(stpx);
                    vbary.add(stpy);

                    if(debugMode==1){
                        System.out.format(" v0     %3d %3d   --> %s \n",i,j,v0.toStringBrief(2));
                        System.out.format(" stpx   %3d %3d   --> %s \n",i,j,stpx.toStringBrief(2));
                        System.out.format(" vb     %3d %3d   --> %s \n",i,j,vb.toStringBrief(2));
                        System.out.format(" vcross %3d %3d   --> %s \n",i,j,vcross.toStringBrief(2));
                        System.out.format(" vspy   %3d %3d   --> %s \n",i,j,stpy.toStringBrief(2));
                        System.out.format(" vbary  %3d %3d   --> %s \n",i,j,vbary.toStringBrief(2));
                    }

                    found=1;
               
                }
            }
        }

        Vector3D step = new Vector3D(0.,0.,0.);
        Vector3D vtx  = new Vector3D(0.,0.,0.);
        step.add(stpx);
        step.add(stpy); 
        step.scale(-1.); 
        double phi = step.phi()*RAD;
        if(debugMode==1)System.out.format(" step  --> %s  %7.2f\n",step.toStringBrief(2),phi);

        if(phi>0 && phi<=90) {
            vtx = v0.toVector3D();
        }
        if(phi>-180  && phi<=-90) { 
            vtx = (vbary.sub(step)).clone();
            stpx.scale(-1);
            stpy.scale(-1);
        }
        if(phi>90  && phi<=180) {
            vcross.rotate(step,90/RAD);
            vtx = (vbary.add(step)).clone();
            stpx.scale(-1);
        }
        if(phi>-90  && phi<=0) { 
            vcross.rotate(step,-90/RAD);
            vtx = (vbary.add(step)).clone();
            stpy.scale(-1);
        }

        Vector3D diff = vtx.toPoint3D().vectorTo(point);
        Vector3D prox = diff.projection(stpx);
        Vector3D proy = diff.projection(stpy);

        if(debugMode==1){
            System.out.format(" \n");
            System.out.format(" diff  %s \n",diff.toStringBrief(2));
            System.out.format(" stpx  %s \n",stpx.toStringBrief(2));
            System.out.format(" prox  %s \n",prox.toStringBrief(2));
            System.out.format(" stpy  %s \n",stpy.toStringBrief(2));
            System.out.format(" proy  %s \n",proy.toStringBrief(2));
            System.out.format(" \n");
        }

        double dx = prox.mag();
        double dy = proy.mag();
        double ddx = dx/20*Nqua;
        double ddy = dy/20*Nqua;
       
        double th = diff.theta();
        double ph = prox.theta();

        int idx = (int) ddx;
        int idy = (int) ddy;

        if(debugMode==1){
            System.out.format(" angles %7.2f %7.2f \n",th,ph);
            System.out.format(" diff  --> %s\n",diff.toStringBrief(2));
            System.out.format(" prox  --> %s\n",prox.toStringBrief(2));
            System.out.format(" proy  --> %s\n",proy.toStringBrief(2));
            System.out.format(" vtx   --> %s\n",vtx.toStringBrief(2));
            System.out.format(" point --> %s   --> %7.2f %7.2f  %7.2f %7.2f  %4d %4d\n",point.toStringBrief(2), dx,dy,ddx,ddy,idx,idy);
            if(idx>=Nqua || idy>=Nqua)System.out.format(" ECCOLO x %7.2f %4d y %7.2f %4d \n",ddx,idx,ddy,idy);  
        }

        // to stay within limits
        if(idx<0)idx=0; 
        if(idx>Nqua-1)idx=Nqua-1; 
        if(idy<0)idy=0; 
        if(idy>Nqua-1)idy=Nqua-1; 

        return idy*Nqua+idx;
    }

    //------------------------------
    public int get_Quadrant(int Nqua, int icompo, Point3D point){
    //------------------------------

        /*
        *  Look for the quadrant of aerogel tile
        */
        int debugMode = 0;

        if(Nqua<0 || Nqua>15)Nqua=1;
        int Nqua2 = (int) Math.pow(Nqua,2);

        if(debugMode>=1)System.out.format(" Get %d %d quadrant for compo %d ilay %d point %s\n", Nqua, Nqua2,icompo,id, point.toStringBrief(2));

        Shape3D surf = get_TrackingSurf(icompo);
        Vector3D vers = new Vector3D(vinside);
        int iqua = get_TileQuadrant(Nqua, icompo, point, select_Vertexes(surf, vers));

        return iqua;

    }



    //------------------------------
    public boolean into_Layer(Line3D ray, int icompo, int ifa) {
    //------------------------------

        if(ray.direction().dot(get_FaceNormal(icompo, ifa))<0)return true;
        return false;
    }


    //------------------------------
    public boolean outfrom_Layer(Line3D ray, int icompo, int ifa) {
    //------------------------------

        if(ray.direction().dot(get_FaceNormal(icompo, ifa))>0)return true;
        return false;
    }

    // ----------------
    public RICHIntersection find_Entrance(Line3D ray, int ico){
    // ----------------

        RICHIntersection test = find_Intersection(ray, ico, 0, 1, 0);
        if(test==null) test = find_Intersection(ray, ico, 0, 0, 0);

        return test;

    }

    // ----------------
    public RICHIntersection find_EntranceCurved(Line3D ray, int ico){
    // ----------------

        return find_Intersection(ray, ico, 0, 1, 1);

    }

    // ----------------
    public RICHIntersection find_Exit(Line3D ray, int ico){
    // ----------------

        RICHIntersection test = find_Intersection(ray, ico, 1, 1, 0);
        if(test==null) test = find_Intersection(ray, ico, 1, 0, 0);

        return test;
    }

    // ----------------
    public RICHIntersection find_ExitCurved(Line3D ray, int ico){
    // ----------------

        return find_Intersection(ray, ico, 1, 1, 1);

    }

    // ----------------
    public RICHIntersection find_Intersection(Line3D ray, int ico, int exit, int post, int curved){
    // ----------------
        /*
        * Search for the intersection points of ray with the RICH component  
        * exit=0 Take the first (in z) with track pointing inside as Entrance
        * exit=1 Take the last (in z) with track pointing outside as Exit
        * post=0 Take the intersection in backward direction
        * post=1 Take the intersection in forward direction
        * curved=0 Stop at the plane
        * curved=1 Refine with curved surface
        */
        //ATT:aggiungere min path

        int debugMode = 0;
        if(ico<-2 || ico>=this.size()) return null;

        boolean global = true;
        int ilay = id;
        Shape3D plane = this.get_GlobalSurf();
        String  splane = "global surf";
        Vector3D glnorm = this.get_LayerNormal(vinside);

        /*
        *  Need compo for aerogel to get ref index
        */
        if((this.is_aerogel() || this.is_spherical_mirror()) && ico==-1){
        //if(this.is_aerogel() && ico==-1)
            global = false;
            splane = "compo  surf";
            plane = this.get_TrackingSurf();
        }

        /*
        *  Take all the intersection with Layer
        */
        RICHIntersection intersection = null;
        List<Point3D> inters = new ArrayList<Point3D>();
        List<Integer> ifaces = new ArrayList<Integer>();

        int nint = plane.intersection_with_faces(ray, inters, ifaces);
        if(debugMode>=1) {
            String ee="out";
            if(exit==0) ee="into";
            String ef="forw";
            if(post==0) ef="back";
            System.out.format("Find intersection (%s, %s) with %s of layer %d %s : %d on faces ",ef,ee,splane,id,name,nint);
            for (int ii=0; ii<nint; ii++)System.out.format(" %4d (%4d) | ",ifaces.get(ii),this.get_CompoIndex(ii));
            System.out.format(" \n");
        }

        double vers = ray.direction().costheta();
        Point3D point = new Point3D(0.0,0.0,0.0);
        if((exit==0 && vers>0) || (exit==1 && vers<0))point.setZ(999.);
        
        /*
        *  Select the best intersection 
        */
        for (int ii=0; ii<nint; ii++){

            Point3D G4inter = inters.get(ii);
            double Delta_z = G4inter.z()-ray.origin().z();
            int iface = ifaces.get(ii);
            int ifacompo = this.get_CompoIndex(iface);
            Vector3D norm = this.get_FaceNormal(ifacompo, iface);
            if(global){
                ifacompo = -1;
                norm = this.get_FaceNormal(-1, iface);
            }
            if(debugMode>=1)  System.out.format(" ila %3d ico %3d ifa %3d:  pos %s  (z range %7.2f : %7.2f)  vers %7.3f cfr %7.3f  normal %s glnorm %s\n",
                              id,ifacompo,iface,G4inter.toStringBrief(2),ray.origin().z(),point.z(),vers,Delta_z, norm.toStringBrief(3),
                              glnorm.toStringBrief(3));

            if(G4inter.distance(ray.origin())<geopar.MIN_RAY_STEP){if(debugMode>=1)System.out.format("     --> too close \n"); continue;}
            if(post==1){
                if(vers*Delta_z<0){if(debugMode>=1)System.out.format("     --> wrong progression \n"); continue;}
            }else{
                if(vers*Delta_z>0){if(debugMode>=1)System.out.format("     --> wrong progression \n"); continue;}
            }
            if(exit==1){
                if(into_Layer(ray,ifacompo, iface)){if(debugMode>=1)System.out.format("     --> wrong direction\n"); continue;}
            }else{
                if(outfrom_Layer(ray,ifacompo, iface)){if(debugMode>=1)System.out.format("     --> wrong direction\n"); continue;}
            }

            Delta_z = G4inter.z()-point.z();

            if(exit==0 && vers*Delta_z <0 ){
                // take the first (in z) entrance point
                if(debugMode>=1)  System.out.format("     --> ok as moving into Shape %7.2f %7.2f \n", G4inter.z(), point.z());
                point = G4inter;
                int icompo = ico;
                if(icompo==-1)icompo=ifacompo;
                    
                intersection = new RICHIntersection(sector, ilay, icompo, iface, exit, point, norm);
                if(this.is_aerogel()){ 
                    if(icompo>=0) intersection.set_nout(get(icompo).get_index());
                    if(debugMode>=1)System.out.format("         --> save aerogel entrance for ilay %4d icompo %4d (%4d %4d) nindx %7.5f \n",
                                     ilay,icompo,ico,ifacompo,intersection.get_nout());
                }
            }

            if(exit==1 && vers*Delta_z >0 ){
                // take the last (in z) exiting point
                if(debugMode>=1)  System.out.format("     --> ok as moving out of Shape %7.2f %7.2f \n", G4inter.z(), point.z());
                point = G4inter;
                int icompo = ico;
                if(icompo==-1)icompo=ifacompo;

                intersection = new RICHIntersection(sector, ilay, icompo, iface, exit, point, norm);
                if(this.is_aerogel()){
                    if(icompo>=0)intersection.set_nin(get(icompo).get_index());
                    if(debugMode>=1)System.out.format("         --> save aerogel exit     for ilay %4d icompo %4d (%4d %4d) nindx %7.5f \n",
                                          ilay,icompo, ico, ifacompo, intersection.get_nin());
                }
            }

        }

        /*
        *  Correct for non planar shape
        */
        if(curved==1 && intersection!=null && (this.is_aerogel() || this.is_spherical_mirror())){
            int icompo = intersection.get_component();
            RICHIntersection new_inter = null;

            Sphere3D sphere = this.get_TrackingSphere(icompo);
            List<Point3D> crosses = new ArrayList<Point3D>();
            int ncross = sphere.intersection(ray, crosses);
            for (int ic=0; ic<ncross; ic++){
                Point3D new_point = crosses.get(ic);
                if(debugMode>=1)System.out.format(" cross with sphere %s \n",new_point.toStringBrief(2));
                if(new_point.distance(intersection.get_pos())<geopar.MAX_SPHE_DIST){

                    Vector3D new_norm = sphere.getNormal(new_point.x(), new_point.y(), new_point.z()).asUnit().multiply(-1.0);
                    new_inter = new RICHIntersection(sector, ilay, icompo, 0, exit, new_point, new_norm);

                }
            }
            if(new_inter!=null){
                if(debugMode>=1){
                    System.out.format(" reset intersection \n");
                    System.out.format(" pos  %s --> %s \n", intersection.get_pos().toStringBrief(2), new_inter.get_pos().toStringBrief(2));
                    System.out.format(" norm %s --> %s \n", intersection.get_normal().toStringBrief(3), new_inter.get_normal().toStringBrief(3));
                }
                intersection.set_pos(new_inter.get_pos());
                intersection.set_normal(new_inter.get_normal());
            }
        }

        if(debugMode>=3){
            if(exit==0)System.out.println(" entrance point "+intersection.get_pos()+"   normal "+intersection.get_normal());
            if(exit==1)System.out.println(" exit     point "+intersection.get_pos()+"   normal "+intersection.get_normal());
        }

        return intersection;

    }

    //------------------------------
    public Vector3D get_SurfMainAx(int icompo, Vector3D vers, Vector3D vref){
    //------------------------------

        int debugMode = 0;

        Shape3D surf = null;
        if(icompo<0 || icompo>=this.size()){
            surf=this.get_GlobalSurf();
        }else{
            surf=this.get_TrackingSurf(icompo);
        }
        if(debugMode==1)System.out.format("get_SurfMainAx \n");
        if (surf==null) return null;
      
        Vector3D va = new Vector3D();
        Vector3D vm = new Vector3D();
        Vector3D main_axes = new Vector3D();
        Vector3D bary = get_SurfBary(icompo, vers);
        List<Point3D> pts = select_Vertexes(surf, vers);
        
        if(debugMode==1)System.out.format("MA Check %d  %s vref --> %s %s \n",icompo, vers.toStringBrief(2), vref.toStringBrief(2), bary.toStringBrief(2));

        double angmin = 999.;
        double angcut = 30.;
        double lmax = 0.;
        for(int ip=0; ip<pts.size(); ip++){
            Point3D p = pts.get(ip);
            for(int iq=ip+1; iq<pts.size(); iq++) {
                Point3D q = pts.get(iq);
                if (p.distance(q)>1e-3){
                    vm=p.midpoint(q).toVector3D().sub(bary);
                    if(debugMode==1)System.out.format("MA %3d %s %3d %s --> %s %s %7.2f %7.2f \n", 
                             ip,p.toStringBrief(2),iq,q.toStringBrief(2),p.midpoint(q).toStringBrief(2),vm.toStringBrief(2),vm.mag(),vm.angle(vref)/Math.PI*180);

                    if (vm.angle(vref)/Math.PI*180<angcut && vm.mag()>lmax){
                        if(debugMode==1)System.out.format(" -->  take this \n");
                        lmax=vm.mag();
                        va=vm;
                        angmin=vm.angle(vref)/Math.PI*180;
                    }
                }
            }
        }
        if(debugMode==1)System.out.format(" ANGMIN %7.2f \n",angmin);

        return va.asUnit();

    }

    // ----------------
    public RICHFrame generate_LocalRef() { return generate_LocalRef(-1, vinside); }
    // ----------------

    // ----------------
    public RICHFrame generate_LocalRef(int icompo) { return generate_LocalRef(icompo, vinside); }
    // ----------------

    // ----------------
    public RICHFrame generate_LocalRef(int icompo, Vector3D vers) {
    // ----------------

        int debugMode = 0;

        Vector3D xref = new Vector3D();
        Vector3D yref = new Vector3D();
        Vector3D zref = new Vector3D();
        Vector3D bref = new Vector3D();
 
        /*
        *   Take global surf if icompo=-1
        */
        if(debugMode==1)System.out.format("LR %3d %s \n",icompo,vers.toStringBrief(2));
        bref = this.get_SurfBary(icompo, vers);
        if(debugMode==1)System.out.format("LR bref  %s \n",bref.toStringBrief(2));

        if(this.is_spherical_mirror()){
            zref = get_TrackingSphere(icompo).getNormal(bref.x(), bref.y(), bref.z()).asUnit();
            Vector3D vref = new Vector3D(0., 1., 0.);
            yref = get_SurfMainAx(icompo, vers, vref);
            xref = (yref.cross(zref)).asUnit();
        }else{

            if(this.is_lateral_mirror()){
                zref = get_CompoNormal(icompo, vers);
                Vector3D vref = new Vector3D(0., 0., 1.);
                yref = get_SurfMainAx(icompo, vers, vref);
                xref = (yref.cross(zref)).asUnit();
            }else{
                zref = get_CompoNormal(icompo, vers);
                Vector3D vref = new Vector3D(0., 1., 0.);
                if(debugMode==1)System.out.format("LR Check %d  %s --> %s %s \n",icompo, vers.toStringBrief(2), zref.toStringBrief(2), vref.toStringBrief(2));
                yref = get_SurfMainAx(icompo, vers, vref);
                xref = (yref.cross(zref)).asUnit();
            }
        }

        if(debugMode==1){
            System.out.format("LOCAL ref: xref %s  yref %s  zref %s \n",xref.toStringBrief(2), yref.toStringBrief(2), zref.toStringBrief(2));
        }

        local_frame = new RICHFrame(xref, yref, zref, bref);
        return local_frame;

    }

    // ----------------
    public boolean CheckSphere() { return CheckSphere(-1);}
    // ----------------

    // ----------------
    public boolean CheckSphere(int ico) { 
    // ----------------

        int debugMode = 0;

        Point3D center = get_TrackingSphere(ico).getCenter();
        double radius = get_TrackingSphere(ico).getRadius();
        Shape3D surf = new Shape3D();
        if(ico<0 || ico>=this.size()){
            surf = get_GlobalSurf();
        }else{
            surf = get_TrackingSurf(ico);
        }
        if(surf==null)return false;

	List<Point3D> pts = select_Vertexes(surf, vinside);
        
        double diffmax=0;
        for(Point3D p:pts){
            double diff = p.distance(center)-radius;
            if(debugMode>=1)System.out.format("Check ila %3d ico %3d point %s: %7.3f \n",id,ico,p.toStringBrief(2),diff);
            if(diff>diffmax)diffmax=diff;
        }

        if(diffmax>1)return false;
        return true;

    }

    /*
    // ----------------
    public Vector3D get_Xref() { return this.xref;}
    // ----------------

    // ----------------
    public Vector3D get_Yref() { return this.yref;}
    // ----------------

    // ----------------
    public Vector3D get_Zref() { return this.zref;}
    // ----------------

    // ----------------
    public Vector3D get_Bref() { return this.bref;}
    // ----------------
    */

    // ----------------
    public boolean is_2cm_aerogel() {
    // ----------------

        if( id==RICHLayerType.AEROGEL_2CM_B1.id() || id==RICHLayerType.AEROGEL_2CM_B2.id() ) return true;
        return false;

    }


    // ----------------
    public boolean is_3cm_aerogel() {
    // ----------------

        if( id==RICHLayerType.AEROGEL_3CM_L1.id() || id==RICHLayerType.AEROGEL_3CM_L2.id() ) return true;
        return false;

    }


    // ----------------
    public boolean is_aerogel() {
    // ----------------

        if( is_2cm_aerogel() || is_3cm_aerogel() ) return true;
        return false;

    }

    // ----------------
    public boolean is_spherical_mirror() { 
    // ----------------

        if( id==RICHLayerType.MIRROR_SPHERE.id()) return true;
        return false;

    }

    // ----------------
    public boolean is_planar_mirror() { 
    // ----------------

        if( is_front_mirror() || is_lateral_mirror() ) return true;
        return false;
    }


    // ----------------
    public boolean is_front_mirror() { 
    // ----------------

        if( id==RICHLayerType.MIRROR_FRONT_B1.id() || id==RICHLayerType.MIRROR_FRONT_B2.id() ) return true;
        return false;
    }


    // ----------------
    public boolean is_lateral_mirror() { 
    // ----------------

        if( id==RICHLayerType.MIRROR_LEFT_L1.id()  || id==RICHLayerType.MIRROR_LEFT_L2.id()  ||
            id==RICHLayerType.MIRROR_RIGHT_R1.id() || id==RICHLayerType.MIRROR_RIGHT_R2.id() ||
            id==RICHLayerType.MIRROR_BOTTOM.id() ) return true;
        return false;
    }


    // ----------------
    public boolean is_mirror() { 
    // ----------------

        if( is_planar_mirror() || is_spherical_mirror() ) return true;
        return false;
    }


    // ----------------
    public boolean is_mapmt() { 
    // ----------------

        if( id==RICHLayerType.MAPMT.id() ) return true;
        return false;
    }


    // ----------------
    public boolean is_optical() { return this.get(0).is_optical(); } 
    // ----------------


    // ----------------
    public void merge_Shape3D(Shape3D shape, Shape3D other) {
    // ----------------

        for(int ifa=0; ifa<other.size(); ifa++)shape.addFace( other.face(ifa) );

    }

    // ----------------
    public Shape3D merge_CompoSurfs() {
    // ----------------

        Shape3D merge = new Shape3D();
        for (int ico=0; ico<this.size(); ico++){
            merge_Shape3D(merge, this.get_TrackingSurf(ico));
        }
        return merge;
    }

    // ----------------
    public ArrayList<Integer> merge_CompoList() {
    // ----------------

        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int ico=0; ico<this.size(); ico++){
            Shape3D shape = this.get_TrackingSurf(ico);
            for (int ifa=0; ifa<shape.size(); ifa++){
                list.add(ico);
            }

        }
        return list;
    }


    //------------------------------
    public Point3D toPoint3D(Vertex ver) {return  new Point3D(ver.pos.x, ver.pos.y, ver.pos.z); }
    //------------------------------

    //------------------------------
    public Point3D toPoint3D(Vector3D ver) {return  new Point3D(ver.x(), ver.y(), ver.z()); }
    //------------------------------

    //------------------------------
    public Vector3d toVector3d(Vertex ver) {return  new Vector3d(ver.pos.x, ver.pos.y, ver.pos.z); }
    //------------------------------

    //------------------------------
    public Vector3d toVector3d(Vector3D ver) {return  new Vector3d(ver.x(), ver.y(), ver.z()); }
    //------------------------------

    //------------------------------
    public Vector3d toVector3d(Point3D pos) {return  new Vector3d(pos.x(), pos.y(), pos.z()); }
    //------------------------------

    //------------------------------
    public Vector3D toVector3D(Vector3d ver) {return  new Vector3D(ver.x, ver.y, ver.z); }
    //------------------------------

    //------------------------------
    public Triangle3D toTriangle3D(Face3D face){
    //------------------------------

        return new Triangle3D(face.point(0), face.point(1), face.point(2));

    }

    // ----------------
    public void show_Layer() {
    // ----------------
        System.out.format("Layer id %3d  size %4d \n", this.id, this.get_size()); 
        this.nominal_plane.show();
        for(int j = 0; j< this.size(); j++) {
            System.out.format("  --> comp # %3d  id %3d  voltype %3d  optical %3d  mirror %3d  n %6.3f \n",
                  j, this.get(j).get_id(), this.get(j).get_voltype(), this.get(j).get_optical(), this.get(j).get_type(), this.get(j).get_index());
        }
    }

}
