import com.wap.core.*; import com.wap.core.Robot; import com.wap.render.*;
import java.awt.*; import java.awt.geom.*; import java.awt.image.BufferedImage; import javax.imageio.ImageIO; import java.io.File; import java.util.*;
/** Pruebas de escritorio: simula partidas sin dispositivo y genera capturas. */
public class Sim {
    static class J implements Gfx {
        Graphics2D c, main; BufferedImage low; boolean pix; final int W, H; final HashMap<Integer, BufferedImage> tiles = new HashMap<>(); final ArrayDeque<AffineTransform> st = new ArrayDeque<>();
        J(BufferedImage b) { c = b.createGraphics(); W = b.getWidth(); H = b.getHeight(); c.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); c.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON); }
        float sw(float w){return pix&&w<1.5f?0:w;}
        ArrayDeque<Graphics2D> gs=new ArrayDeque<>();ArrayDeque<BufferedImage> ims=new ArrayDeque<>();
        public void beginPixels(int w,int h){BufferedImage im=new BufferedImage(Math.max(1,w),Math.max(1,h),BufferedImage.TYPE_INT_ARGB);gs.push(c);ims.push(im);c=im.createGraphics();pix=true;}
        public void endPixels(float x,float y,int s){BufferedImage im=ims.pop();c.dispose();c=gs.pop();pix=!ims.isEmpty();AffineTransform t=c.getTransform();c.translate(x,y);c.scale(s,s);c.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);c.drawImage(im,0,0,null);c.setTransform(t);}
        public int w(){return W;} public int h(){return H;}
        public void save(){st.push(c.getTransform());} public void restore(){c.setTransform(st.pop());}
        public void translate(float x,float y){c.translate(x,y);} public void rotate(float r){c.rotate(r);} public void scale(float x,float y){c.scale(x,y);}
        Color col(int a){return new Color(a,true);}
        public void clear(int k){AffineTransform t=c.getTransform();c.setTransform(new AffineTransform());c.setColor(col(k));c.fillRect(0,0,W,H);c.setTransform(t);}
        public void grad(float[] p,int a,int b,float x0,float x1,int s,float sw){Path2D.Float q=new Path2D.Float();q.moveTo(p[0],p[1]);for(int i=2;i<p.length;i+=2)q.lineTo(p[i],p[i+1]);q.closePath();c.setPaint(new GradientPaint(x0,0,col(a),x1,0,col(b)));c.fill(q);c.setPaint(col(s));c.setStroke(new BasicStroke(sw(sw),1,1));c.draw(q);}
        public void poly(float[] p,int f,int s,float sw){Path2D.Float q=new Path2D.Float();q.moveTo(p[0],p[1]);for(int i=2;i<p.length;i+=2)q.lineTo(p[i],p[i+1]);q.closePath();if(f!=0){c.setColor(col(f));c.fill(q);}if(s!=0){c.setColor(col(s));c.setStroke(new BasicStroke(sw(sw),1,1));c.draw(q);}}
        public void circle(float x,float y,float r,int f,int s,float sw){Ellipse2D.Float e=new Ellipse2D.Float(x-r,y-r,r*2,r*2);if(f!=0){c.setColor(col(f));c.fill(e);}if(s!=0){c.setColor(col(s));c.setStroke(new BasicStroke(sw(sw)));c.draw(e);}}
        public void line(float a,float b,float d,float e,int k,float w){c.setColor(col(k));c.setStroke(new BasicStroke(sw(w),1,1));c.draw(new Line2D.Float(a,b,d,e));}
        public void rect(float x,float y,float w,float h,int k){c.setColor(col(k));c.fill(new Rectangle2D.Float(x,y,w,h));}
        public void rrect(float x,float y,float w,float h,float r,int f,int s,float sw){RoundRectangle2D.Float q=new RoundRectangle2D.Float(x,y,w,h,r*2,r*2);if(f!=0){c.setColor(col(f));c.fill(q);}if(s!=0){c.setColor(col(s));c.setStroke(new BasicStroke(sw(sw)));c.draw(q);}}
        public void text(String s,float x,float y,float z,int k,int a){c.setFont(new Font("SansSerif",Font.BOLD,12).deriveFont(z));c.setColor(col(k));float w=c.getFontMetrics().stringWidth(s);c.drawString(s,a==0?x-w/2:a>0?x-w:x,y);}
        public float textW(String s,float z){c.setFont(new Font("SansSerif",Font.BOLD,12).deriveFont(z));return c.getFontMetrics().stringWidth(s);}
        public void tile(int id,int[] px,int S,boolean ch,float x,float y){BufferedImage b=tiles.get(id);if(b==null){b=new BufferedImage(S,S,BufferedImage.TYPE_INT_ARGB);tiles.put(id,b);ch=true;}if(ch)b.setRGB(0,0,S,S,px,0,S);c.drawImage(b,(int)x,(int)y,null);}
    }
    static Store store=new Store(){HashMap<String,String> m=new HashMap<>();public String get(String k,String d){return m.getOrDefault(k,d);}public void put(String k,String v){m.put(k,v);}};
    static Audio mute=i->{};
    static Wpn[] set(String... n){Wpn[] r=new Wpn[Robot.SLOTS];for(int i=0;i<Robot.SLOTS;i++)r[i]=Wpn.ALL[Wpn.index(n[i])];return r;}
    static void check(boolean ok,String s){System.out.println((ok?"OK    ":"FALLA ")+s);if(!ok)fails++;} static int fails;
    static World flat(){World w=new World(5,10,set("GRENADE","BAZOOKA","SHOTGUN","MINE","CHAINGUN"),set("GRENADE","BAZOOKA","SHOTGUN","MINE","CHAINGUN"));
        Arrays.fill(w.t.m,(byte)0);for(int y=600;y<1000;y++)for(int x=0;x<1800;x++)w.t.m[y*1800+x]=1;for(int y=0;y<1000;y++)for(int x=0;x<1800;x++)if(x<14||x>1785)w.t.m[y*1800+x]=2;
        Robot a=w.robots[0],b=w.robots[1];a.px=500;a.py=600;a.facing=1;b.px=1500;b.py=600;a.invuln=b.invuln=0;for(int i=0;i<3;i++)w.step(new int[2]);return w;}
    static void run(World w,int m,int n){int[] in={m,0};for(int i=0;i<n;i++)w.step(in);}
    static void mech(){
        World w=flat();Robot r=w.robots[0];check(r.grounded,"el robot queda sobre el suelo");
        int x=r.px;run(w,Input.RIGHT,40);check(r.px>x+60,"derecha camina ("+(r.px-x)+" px)");
        run(w,0,20);run(w,Input.RIGHT,30);int sh=w.shots.size();run(w,Input.RIGHT|Input.A,1);run(w,Input.RIGHT,8);check(w.shots.size()>sh,"corriendo + A dispara el arma 5");check(w.robots[0].ammo[3]==w.robots[0].loadout[3].clip-1,"gasta munición del arma 5");
        run(w,0,30);run(w,Input.RIGHT,30);int y=r.py;run(w,Input.RIGHT|Input.B,6);check(r.py<y,"corriendo + B salta");run(w,0,70);
        w=flat();r=w.robots[0];run(w,0,5);run(w,Input.RIGHT,1);run(w,Input.RIGHT|Input.A,1);run(w,0,8);check(r.ammo[1]==r.loadout[1].clip-1,"toque lateral + A = arma 2");
        w=flat();r=w.robots[0];int a4=r.ammo[3];int yb=r.py;run(w,Input.LEFT,1);run(w,Input.LEFT|Input.B,1);run(w,Input.LEFT|Input.B,8);check(r.py<yb&&w.shots.isEmpty()&&r.ammo[3]==a4,"lado + B ahora solo salta");
        w=flat();r=w.robots[0];run(w,Input.DOWN,1);run(w,Input.DOWN|Input.A,1);run(w,0,8);check(r.ammo[0]==r.loadout[0].clip-1,"abajo + A = arma 1");
        w=flat();r=w.robots[0];run(w,Input.UP,1);run(w,Input.UP|Input.A,1);run(w,0,8);check(r.ammo[2]==r.loadout[2].clip-1,"arriba + A = arma 3");
        w=flat();r=w.robots[0];run(w,Input.DOWN,1);run(w,Input.DOWN|Input.B,1);check(r.sliding(),"abajo + B desliza");x=r.px;run(w,Input.DOWN,15);check(r.h==Robot.SLIDE_H&&r.px>x+40,"el deslizamiento baja la altura y avanza");
        w=flat();r=w.robots[0];run(w,0,1);run(w,Input.A,1);run(w,0,12);check(r.ammo[3]==r.loadout[3].clip-1,"A sola = arma 5");
        w=flat();r=w.robots[0];int yy=r.py;run(w,Input.B,1);run(w,Input.B,8);check(r.py<yy,"B sola salta");
        w=flat();for(int y2=420;y2<440;y2++)for(int x2=400;x2<900;x2++)w.t.m[y2*1800+x2]=1;r=w.robots[0];run(w,Input.UP,1);run(w,Input.UP|Input.B,1);for(int i=0;i<40&&r.rope!=2;i++)run(w,Input.B,1);check(r.rope==2,"arriba + B engancha manteniendo B");
        yy=r.py;run(w,Input.B,60);check(r.py<yy-60&&r.rope==2,"el gancho lleva solo al destino ("+(yy-r.py)+" px)");int hold=r.py;run(w,Input.B,30);check(Math.abs(r.py-hold)<4,"se queda colgado mientras mantiene B");run(w,0,30);check(r.rope==0&&r.py>hold+20,"soltar B suelta el gancho y cae");
        w=flat();r=w.robots[0];run(w,Input.UP,1);run(w,Input.UP|Input.B,1);run(w,0,2);check(r.rope==0,"soltar antes de enganchar cancela el gancho");
        w=flat();r=w.robots[0];int b4;for(int y2=100;y2<600;y2++)for(int x2=r.px+8;x2<r.px+60;x2++)w.t.m[y2*1800+x2]=1;b4=count(w);run(w,Input.RIGHT,80);check(count(w)==b4,"el robot ya no escarba");run(w,Input.DOWN,60);run(w,Input.UP,60);check(count(w)==b4,"arriba y abajo tampoco escarban");
        w=flat();r=w.robots[0];Shot sh2=null;run(w,Input.UP,1);run(w,Input.UP|Input.B,1);boolean recto=true;w.robots[0].loadout[2]=Wpn.ALL[Wpn.index("BAZOOKA")];w=flat();r=w.robots[0];r.loadout[2]=Wpn.ALL[Wpn.index("BAZOOKA")];r.loadout[0]=r.loadout[2];run(w,Input.UP,1);run(w,Input.UP|Input.A,1);run(w,0,8);for(Shot q:w.shots)recto&=Math.abs(q.vy)<.5f;run(w,0,50);run(w,Input.DOWN,1);run(w,Input.DOWN|Input.A,1);run(w,0,8);for(Shot q:w.shots)recto&=Math.abs(q.vy)<.5f;check(recto&&w.shots.size()>0,"arriba y abajo también disparan recto");
        w=flat();r=w.robots[0];int gy=r.py;run(w,Input.B,1);int top=gy;for(int i=0;i<80;i++){run(w,Input.B,1);top=Math.min(top,r.py);}check(gy-top>=130,"salto más alto ("+(gy-top)+" px)");
        w=flat();r=w.robots[0];for(int y2=100;y2<560;y2++)for(int x2=540;x2<580;x2++)w.t.m[y2*1800+x2]=1;for(int y2=100;y2<560;y2++)for(int x2=420;x2<460;x2++)w.t.m[y2*1800+x2]=1;r.px=500;r.py=300;r.vx=0;r.vy=0;run(w,Input.RIGHT,30);float slideV=r.vy;check(r.wallDir==1&&slideV<=Robot.WALL_SLIDE,"se afirma en la pared y baja lento");int ww=r.py;run(w,0,1);run(w,Input.B,1);check(r.vy<-5&&r.vx<0,"salto en pared rebota hacia afuera");run(w,Input.B,12);run(w,Input.LEFT|Input.B,28);run(w,Input.LEFT,12);check(r.wallDir==-1,"alcanza la pared opuesta");run(w,Input.LEFT|Input.B,1);check(r.vx>0&&r.vy<-5,"vuelve a rebotar entre paredes");
        w=flat();r=w.robots[0];for(int i=0;i<Robot.SLOTS;i++){r.loadout[i]=Wpn.ALL[Wpn.index("PLASMA CANNON")];r.ammo[i]=1;}int sc=0;run(w,Input.UP,1);run(w,Input.UP|Input.A,1);run(w,Input.UP,1);check(w.shots.isEmpty()&&r.charge[2]>0,"plasma cannon empieza a cargar sin disparar");run(w,0,100);check(w.shots.isEmpty()&&r.chargeLevel()>=1,"plasma cannon queda cargada esperando");run(w,Input.UP,1);run(w,Input.UP|Input.A,1);run(w,0,8);check(w.shots.size()==1&&r.chargeLevel()==0,"el segundo comando la lanza");
        w=flat();r=w.robots[0];for(int i=0;i<Robot.SLOTS;i++)r.loadout[i]=Wpn.ALL[Wpn.index("PLASMA GUN")];run(w,0,1);run(w,Input.A,4);check(w.shots.isEmpty()&&r.windT>0,"antes del disparo hay una preparación corta");run(w,Input.A,6);check(w.shots.size()==1&&w.shots.get(0).w.name.equals("PLASMA GUN"),"plasma gun dispara");check(Math.abs(w.shots.get(0).y-(r.py-30))<3&&w.shots.get(0).x-r.px>=18,"el disparo sale de la mano extendida, no de la cintura");run(w,Input.A,12);check(w.shots.size()==2,"en ráfaga no repite la preparación");
        w=flat();for(int y2=300;y2<340;y2++)for(int x2=700;x2<760;x2++)w.t.m[y2*1800+x2]=1;w.t.carveE(730,320,31,16);int deb=0;for(byte q:w.t.m)if(q==Terrain.DEBRIS)deb++;boolean nocol=!w.t.solid(700,301);check(deb>0&&nocol,"trozo pequeño queda sin colisión ("+deb+" px)");
        w=flat();Terrain T=w.t;r=w.robots[0];int[] pq={700,568,32,32,Terrain.CRATE};for(int y2=568;y2<600;y2++)for(int x2=700;x2<732;x2++)T.m[y2*1800+x2]=Terrain.CRATE;w.props.clear();T.props.clear();T.props.add(pq);World tmp=w;Prop pr=mkProp(w,pq);for(int i=0;i<Robot.SLOTS;i++){r.loadout[i]=Wpn.ALL[Wpn.index("UZI")];r.ammo[i]=30;}r.px=640;r.py=600;run(w,Input.RIGHT,1);run(w,0,1);run(w,Input.RIGHT,1);w.spawn(Wpn.ALL[Wpn.index("UZI")],0,690,585,10,0);run(w,0,6);check(pr.hp<pr.max&&!pr.gone&&T.m[580*1800+710]==Terrain.CRATE,"la caja resiste golpes sin perder píxeles ("+pr.hp+"/"+pr.max+")");T.carve(715,585,30);check(T.m[585*1800+715]==Terrain.CRATE,"el recorte de explosión no corta el objeto");for(int i=0;i<30&&!pr.gone;i++){w.spawn(Wpn.ALL[Wpn.index("UZI")],0,690,585,10,0);run(w,0,6);}check(pr.gone&&!T.solid(715,585),"al agotar su resistencia se rompe en pedazos");boolean pc=false;for(Fx f:w.fx)pc|=f.type==Fx.PROP;check(pc,"emite el evento de pedazos");
        w=flat();T=w.t;int[] pz={700,536,32,32,Terrain.METAL};for(int y2=536;y2<568;y2++)for(int x2=700;x2<732;x2++)T.m[y2*1800+x2]=Terrain.METAL;Prop pm=mkProp(w,pz);run(w,0,25);check(pm.gone,"un objeto sin apoyo cae y se rompe");
        w=flat();b4=count(w);w.explode(600,600,36,30,32,3,1);check(count(w)<b4,"la explosión destruye el terreno");
        w=flat();r=w.robots[0];w.robots[1].px=r.px+40;int ac=r.armorCount();w.explode(r.px,r.py-30,36,44,0,3,1);check(r.armorCount()<ac,"el daño arranca piezas de armadura ("+ac+"→"+r.armorCount()+")");
        w=flat();r=w.robots[0];int lv=r.lives;for(int i=0;i<30&&r.lives==lv;i++){r.invuln=0;w.explode(r.px,r.py-30,36,44,0,0,1);}check(r.lives==lv-1&&r.deadT>0,"sin vida pierde una vida");for(int i=0;i<200;i++)w.step(new int[2]);check(r.alive()&&r.hp==100&&r.armorCount()==12,"reaparece con armadura completa");
        w=new World(1,0,set("RIFLE","RIFLE","RIFLE","RIFLE","RIFLE"),set("RIFLE","RIFLE","RIFLE","RIFLE","RIFLE"));r=w.robots[0];r.invuln=0;for(int i=0;i<40&&!w.finished;i++){r.invuln=0;w.explode(r.px,r.py-30,36,44,0,0,1);}check(w.finished&&w.winner==1,"sin vidas termina la partida");
    }
    static Prop mkProp(World w,int[] q){try{java.lang.reflect.Constructor<Prop> c=Prop.class.getDeclaredConstructor(int[].class);c.setAccessible(true);Prop p=c.newInstance((Object)q);w.props.add(p);return p;}catch(Exception e){throw new RuntimeException(e);}}
    static void bodies(){
        World w=flat();Robot a=w.robots[0],b=w.robots[1];b.px=a.px+60;run(w,Input.RIGHT,120);check(b.px-a.px>=14,"los personajes no se encimen al caminar ("+(b.px-a.px)+" px)");
        w=flat();a=w.robots[0];b=w.robots[1];a.px=b.px;a.py=b.py-b.h-1;a.vy=0;a.grounded=false;run(w,0,4);check(a.grounded&&a.py==b.py-b.h&&b.stunT==0,"uno puede pararse sobre el otro sin pisotón");
        w=flat();a=w.robots[0];b=w.robots[1];a.px=b.px+3;a.py=b.py-b.h-60;a.vy=0;float hp=b.hp;for(int i=0;i<40&&b.stunT==0;i++)run(w,0,1);check(b.stunT>0&&b.stunT<=Robot.STUN&&hp-b.hp>0&&hp-b.hp<=Robot.STOMP_DMG+.1f,"caer encima aturde poco y quita poca vida ("+(hp-b.hp)+")");check(a.vy<-3,"el que pisa rebota");int bx=b.px;w.step(new int[]{0,Input.RIGHT});check(b.px==bx,"el aturdido no se mueve");run(w,0,Robot.STUN);bx=b.px;w.step(new int[]{0,Input.RIGHT});w.step(new int[]{0,Input.RIGHT});w.step(new int[]{0,Input.RIGHT});check(b.px>bx,"pasado el aturdimiento puede escapar");a.px=b.px;a.py=b.py-b.h-60;a.vy=0;for(int i=0;i<40&&a.py<b.py-b.h;i++)run(w,0,1);check(b.stunT==0,"no se puede encadenar otro aturdimiento enseguida");
        w=flat();a=w.robots[0];w.robots[1].px=1700;int x=a.px;run(w,Input.RIGHT,90);int walk=a.px-x;w=flat();a=w.robots[0];w.robots[1].px=1700;x=a.px;run(w,Input.RIGHT|Input.X,90);int dash=a.px-x;check(dash>walk*1.3f,"X mantenido + dirección corre ("+walk+" → "+dash+" px)");w=flat();a=w.robots[0];w.robots[1].px=1700;x=a.px;run(w,Input.RIGHT,1);run(w,Input.RIGHT|Input.B,1);run(w,Input.RIGHT|Input.B,88);check(a.px-x<walk*1.15f,"B ya no hace correr");
        w=flat();a=w.robots[0];float a0=a.aim;run(w,Input.X|Input.UP,8);check(a.aim>a0+15&&a.aiming,"mantener X y arriba sube la mira");run(w,Input.X|Input.DOWN,40);check(a.aim<0,"con abajo la baja");float am=a.aim;run(w,0,2);run(w,Input.UP,1);run(w,Input.UP|Input.B,1);check(a.rope==1&&Math.abs(Math.toDegrees(Math.atan2(-a.rvy,Math.abs(a.rvx)))-am)<1,"el gancho sale en el ángulo de la mira ("+am+"°)");
        w=flat();a=w.robots[0];int gy=a.py;run(w,Input.B,1);run(w,Input.B,18);run(w,0,1);int y1=a.py;run(w,Input.B,4);check(a.vy<-6&&a.rollT>0,"doble salto en el aire con enrollado");int top=y1;for(int i=0;i<60;i++){run(w,Input.B,1);top=Math.min(top,a.py);}check(gy-top>190,"el doble salto llega más alto ("+(gy-top)+" px)");run(w,Input.B,1);run(w,0,1);run(w,Input.B,1);check(a.rollT==0||a.vy>-7,"no hay triple salto");
        w=flat();a=w.robots[0];b=w.robots[1];for(int i=0;i<Robot.SLOTS;i++)a.loadout[i]=Wpn.ALL[Wpn.index("DROP")];b.px=a.px+50;hp=b.hp;run(w,0,1);run(w,Input.A,1);run(w,0,8);check(b.thrownT>0&&b.vx<0,"DROP agarra al rival y lo lanza hacia atrás");float hp2=b.hp;for(int i=0;i<90&&b.thrownT>0;i++)run(w,0,1);check(hp2-b.hp>=12&&b.thrownT==0,"al chocar pierde un porcentaje de vida ("+(hp2-b.hp)+")");check(b.px<a.px,"cae del otro lado");
        w=flat();a=w.robots[0];b=w.robots[1];b.px=1700;run(w,Input.DOWN,1);run(w,Input.DOWN|Input.B,1);run(w,Input.DOWN,2);hp=a.hp;w.explode(a.px-20,a.py-10,30,20,0,2,1);check(a.vx>5&&a.slideT==0&&hp-a.hp<14&&hp-a.hp>0,"golpe al deslizarse lanza y quita poca vida ("+(hp-a.hp)+")");
        w=flat();a=w.robots[0];b=w.robots[1];b.px=a.px+40;hp=b.hp;run(w,Input.DOWN,1);run(w,Input.DOWN|Input.B,1);run(w,Input.DOWN,10);check(b.vx>3&&b.hp<hp,"barrida lanza al rival");
    }
    static void online()throws Exception{
        java.net.ServerSocket ss=new java.net.ServerSocket(0);com.wap.net.Link[] hl=new com.wap.net.Link[1];Thread t=new Thread(()->{try{hl[0]=com.wap.net.Link.accept(ss);}catch(Exception e){}});t.start();
        com.wap.net.Link cl=com.wap.net.Link.connect("127.0.0.1",ss.getLocalPort());t.join();com.wap.net.Link hlk=hl[0];
        com.wap.net.Link.Hello h=new com.wap.net.Link.Hello();h.rules=World.RULES;h.seed=777;h.lives=5;h.slots=new int[]{1,2,3,4};h.name="Sala prueba";hlk.send(h);com.wap.net.Link.Hello hr=cl.readHello();
        com.wap.net.Link.Hello c=new com.wap.net.Link.Hello();c.rules=World.RULES;c.slots=new int[]{5,9,12,0};cl.send(c);com.wap.net.Link.Hello cr=hlk.readHello();
        check(hr.seed==777&&hr.name.equals("Sala prueba")&&cr.slots[1]==9,"saludo entre teléfonos");
        Wpn[] A=new Wpn[4],B=new Wpn[4];for(int i=0;i<4;i++){A[i]=Wpn.ALL[h.slots[i]];B[i]=Wpn.ALL[c.slots[i]];}
        World wh=new World(777,5,A,B),wc=new World(777,5,A,B);hlk.startReader();cl.startReader();com.wap.net.Lockstep lh=new com.wap.net.Lockstep(hlk,0),lc=new com.wap.net.Lockstep(cl,1);
        Bot bh=new Bot(1,0,1),bc=new Bot(2,1,1);long t0=System.currentTimeMillis();
        while((wh.tick<3000||wc.tick<3000)&&System.currentTimeMillis()-t0<60000){boolean any=false;if(wh.tick<3000){lh.pump(wh,bh.think(wh));any|=lh.step(wh);wh.fx.clear();}if(wc.tick<3000){lc.pump(wc,bc.think(wc));any|=lc.step(wc);wc.fx.clear();}if(!any)Thread.sleep(0,200000);}
        check(wh.tick==3000&&wc.tick==3000&&wh.hash()==wc.hash()&&!lh.desync&&!lc.desync,"ambos teléfonos simulan la misma partida en línea (3000 ticks)");
        check(wh.robots[0].px!=wh.robots[0].lives,"");cl.close();Thread.sleep(200);check(hlk.closed,"salir avisa al otro teléfono");hlk.close();ss.close();
        try(com.wap.net.Lan.Browser br=new com.wap.net.Lan.Browser();com.wap.net.Lan.Beacon bk=new com.wap.net.Lan.Beacon("Sala de prueba",47812,World.RULES)){java.util.List<com.wap.net.Lan.Room> rs=java.util.Collections.emptyList();for(int i=0;i<30&&rs.isEmpty();i++){Thread.sleep(100);rs=br.rooms();}check(!rs.isEmpty()&&rs.get(0).name.equals("Sala de prueba")&&rs.get(0).port==47812,"la sala aparece en la lista de búsqueda");}
    }
    static int count(World w){int c=0;for(byte b:w.t.m)if(b!=0&&b!=Terrain.DEBRIS)c++;return c;}
    static void weapons(){
        for(Wpn p:Wpn.ALL){World w=flat();for(int i=0;i<Robot.SLOTS;i++)w.robots[0].loadout[i]=p;Robot r=w.robots[0],e=w.robots[1];e.px=r.px+120;e.py=600;
            int before=count(w);float hp=e.hp;for(int t=0;t<3;t++){run(w,0,1);run(w,Input.A,1);run(w,Input.A,40);r.delay[3]=0;r.reload[3]=0;if(r.ammo[3]<=0)r.ammo[3]=1;run(w,0,240);}
            System.out.printf("  %-14s tierra %6d  daño %5.1f  disparos vivos %d%n",p.name,before-count(w),hp-e.hp+100*e.deaths,w.shots.size());}
    }
    static void sheet(String out)throws Exception{int S=7,W=1540,H=420;BufferedImage b=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);J g=new J(b);g.clear(0xFFFFFFFF);g.beginPixels(W/S,H/S);int[] ticks={1,5,8,10,14};
        for(int k=0;k<5;k++){World q=flat();Robot o=q.robots[0];for(int i=0;i<Robot.SLOTS;i++)o.loadout[i]=Wpn.ALL[0];RobotArt.View v=new RobotArt.View();int[] in=new int[2];in[0]=Input.A;int t=0;while(t<ticks[k]){q.step(in);if(q.fx.size()>0){for(Fx f:q.fx)if(f.type==Fx.FLASH)v.shotT=12;q.fx.clear();}RobotArt.update(v,o,q);in[0]=0;t++;}g.save();g.translate(22+k*40,55);RobotArt.drawPose(g,v,o.armor,RobotArt.COLOR[0],0);g.restore();}
        World q=flat();Robot o=q.robots[0];RobotArt.View v=new RobotArt.View();for(int t=0;t<40;t++){q.step(new int[]{Input.RIGHT,0});RobotArt.update(v,o,q);}g.save();g.translate(205,55);RobotArt.drawPose(g,v,o.armor,RobotArt.COLOR[0],0);g.restore();
        q=flat();o=q.robots[0];v=new RobotArt.View();q.step(new int[]{Input.B,0});for(int t=0;t<16;t++){q.step(new int[]{Input.B,0});RobotArt.update(v,o,q);}q.step(new int[]{0,0});for(int t=0;t<5;t++){q.step(new int[]{Input.B,0});RobotArt.update(v,o,q);}g.save();g.translate(205,55);g.restore();g.save();g.translate(185,20);RobotArt.drawPose(g,v,o.armor,RobotArt.COLOR[0],0);g.restore();g.endPixels(0,0,S);ImageIO.write(b,"png",new File(out,"poses.png"));}
    static void update()throws Exception{
        com.sun.net.httpserver.HttpServer h=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);byte[] apk=new byte[70000];new Random(1).nextBytes(apk);boolean[] ua={false};
        h.createContext("/api",x->{ua[0]="WAP-app".equals(x.getRequestHeaders().getFirst("User-Agent"));byte[] b="{\"name\":\"WAP 12\",\"assets\":[{\"name\":\"wap.apk\"}]}".getBytes();x.sendResponseHeaders(200,b.length);x.getResponseBody().write(b);x.close();});
        h.createContext("/malo",x->{x.sendResponseHeaders(403,-1);x.close();});
        h.createContext("/pagina",x->{byte[] b="<h1>WAP 13</h1>".getBytes();x.sendResponseHeaders(200,b.length);x.getResponseBody().write(b);x.close();});
        h.createContext("/apk",x->{x.getResponseHeaders().add("Location","/real");x.sendResponseHeaders(302,-1);x.close();});
        h.createContext("/real",x->{x.sendResponseHeaders(200,apk.length);x.getResponseBody().write(apk);x.close();});h.start();String u="http://127.0.0.1:"+h.getAddress().getPort();
        check(UpdateNet.latest(u+"/api",u+"/pagina")==12&&ua[0],"el actualizador lee la versión publicada");check(UpdateNet.latest(u+"/malo",u+"/pagina")==13,"si la API falla usa la página de la versión");
        File f=File.createTempFile("wap",".apk");int[] pc={0};UpdateNet.download(u+"/apk",f,p->pc[0]=p);check(f.length()==apk.length&&pc[0]==100,"descarga la APK siguiendo la redirección");boolean err=false;try{UpdateNet.latest(u+"/malo",u+"/malo");}catch(java.io.IOException e){err=true;}check(err,"un error de red se informa");f.delete();h.stop(0);}
    static void soak(){
        long t0=System.currentTimeMillis();int finished=0,ex=0,hooks=0,jumps=0;int[] kills=new int[2];
        for(int seed=1;seed<=12;seed++){Rng q=new Rng(seed*77);Wpn[] a=new Wpn[Robot.SLOTS],b=new Wpn[Robot.SLOTS];for(int i=0;i<Robot.SLOTS;i++){a[i]=Wpn.ALL[q.i(Wpn.ALL.length)];b[i]=Wpn.ALL[q.i(Wpn.ALL.length)];}
            World w=new World(seed,5,a,b);Bot x=new Bot(seed,0,.9f),y=new Bot(seed+9,1,.9f);int[] in=new int[2];Robot r0=w.robots[0];
            for(int i=0;i<60*60*4&&!w.finished;i++){in[0]=x.think(w);in[1]=y.think(w);w.step(in);for(Fx f:w.fx){if(f.type==Fx.HOOK&&f.a==0)hooks++;if(f.type==Fx.JUMP&&f.a==0)jumps++;}w.fx.clear();
                for(Robot o:w.robots)if(o.alive()&&o.px<0||o.py<0||o.px>Terrain.W||o.py>Terrain.H+1)ex++;}
            if(w.finished)finished++;kills[0]+=w.robots[0].kills;kills[1]+=w.robots[1].kills;System.out.println("  semilla "+seed+" ticks "+w.tick+" bajas "+w.robots[0].kills+"-"+w.robots[1].kills+" fin "+w.finished);}
        check(ex==0,"nadie sale del mapa");check(kills[0]+kills[1]>12,"los bots se combaten y hay bajas: "+kills[0]+"/"+kills[1]);check(hooks>50&&jumps>500,"los bots saltan ("+jumps+") y usan gancho ("+hooks+")");System.out.println("  terminadas "+finished+"/12, "+(System.currentTimeMillis()-t0)+" ms");
    }
    static void determinism(){int[] h=new int[2];for(int k=0;k<2;k++){Wpn[] a=set("GRENADE","BAZOOKA","SHOTGUN","MINE","CHAINGUN");World w=new World(99,10,a,a);Bot x=new Bot(1,0,1),y=new Bot(2,1,1);int[] in=new int[2];for(int i=0;i<7200;i++){in[0]=x.think(w);in[1]=y.think(w);w.step(in);w.fx.clear();}h[k]=w.hash();}check(h[0]==h[1],"misma semilla y entradas dan la misma partida");}
    static void shots(String out)throws Exception{new File(out).mkdirs();
        App app=new App(store,mute);app.seed(4);int W=1560,H=720;BufferedImage b=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);J g=new J(b);
        app.draw(g);ImageIO.write(b,"png",new File(out,"1_titulo.png"));
        app.screen=App.ARMS;app.draw(g);ImageIO.write(b,"png",new File(out,"2_armas.png"));
        app.openLobbyForTest();app.draw(g);ImageIO.write(b,"png",new File(out,"2c_salas.png"));app.back();app.screen=App.PAINT;app.draw(g);ImageIO.write(b,"png",new File(out,"2b_color.png"));app.screen=App.TITLE;app.start();g.tiles.clear();for(int i=0;i<95;i++)app.tick();app.draw(g);ImageIO.write(b,"png",new File(out,"3_inicio.png"));
        Random r=new Random(3);int s=0;for(int i=0;i<1500;i++){if(i%2==0)app.draw(g);int m=(i/70%2==0?Input.RIGHT:Input.LEFT);if(i%50<4)m|=Input.B;if(i%37==0)m|=Input.A;if(i%97==1)m=0;if(i%97==2)m=Input.DOWN|Input.A;
            if(i%160==20)m=Input.UP;if(i%160>20&&i%160<50)m=Input.UP|Input.B;if(i%300==80)m=Input.UP;if(i%300==81)m=Input.UP|Input.A;if(i%211==5)m=Input.DOWN;if(i%211==6)m=Input.DOWN|Input.B;app.keys=m;app.tick();
            if(app.screen!=App.PLAY)break;if(i==700){for(int q=0;q<14;q++){app.keys=Input.X|Input.UP;app.tick();}app.draw(g);ImageIO.write(b,"png",new File(out,"3b_mira.png"));}if(i>120&&i%(190)==0&&app.w.robots[0].alive()){app.draw(g);ImageIO.write(b,"png",new File(out,"j_"+(s++)+".png"));}}
    }
    public static void main(String[] a)throws Exception{mech();bodies();online();update();weapons();determinism();soak();shots(a.length>0?a[0]:"out");sheet(a.length>0?a[0]:"out");System.out.println("fallas: "+fails);}
}
