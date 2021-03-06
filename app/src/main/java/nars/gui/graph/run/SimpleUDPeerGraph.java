//package nars.gui.graph.run;
//
//import jcog.net.UDPeer;
//import jcog.net.UDPeerSim;
//import jcog.pri.PLink;
//import nars.$;
//import nars.NAR;
//import nars.NARS;
//import nars.concept.Concept;
//import nars.control.Activate;
//import nars.exe.FocusExec;
//import nars.gui.graph.TermSpace;
//import nars.gui.graph.ConceptWidget;
//import org.jetbrains.annotations.Nullable;
//import spacegraph.SpaceGraph;
//import spacegraph.math.v2;
//
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.util.List;
//import java.util.Objects;
//
//import static jcog.Texts.i;
//
///**
// * represents a live UDP mesh network as concepts and links
// */
//public class SimpleUDPeerGraph {
//
//    public static void main(String[] args) throws IOException {
//
//        NAR n = new NARS().get();
//
//        int population = 256;
//
//        SimpleConceptGraph1 s = new SimpleConceptGraph1(n,
//                () -> (((FocusExec) (n.exe)).concepts)
//                        .stream()
//                        .map(x -> x instanceof Activate ? ((Activate) x) : null)
//                        .filter(Objects::nonNull)
//                        .iterator()
//                /* TODO */, population+1, population+1, 4, 8);
//
//        new SpaceGraph(s).camPos(0,0,200).show(800, 800);
//
//        UDPeerSim u = new UDPeerSim(population) {
//
//            int WorldX = 300;
//            int WorldY = 200;
//            float KmPerSec = 32000;
//
//            final List<v2> locations = $.newArrayList();
//
//            {
//                for (int i = 0; i < peer.length; i++) {
//                    locations.add(new v2(
//                            (float) (Math.sin(4f * n.random().nextFloat()) * WorldX - WorldX/2),
//                            (float) (Math.cos(4f * n.random().nextFloat()) * WorldY - WorldY/2)));
//                    peer[i].them.setCapacity(8);
//                }
//
//                s.nodeBuilder = (x) -> {
//                    int i = i(x.term().toString()) - 10000;
//                    return new ConceptWidget(x) {
//                        final MyUDPeer pp = peer[i];
//                        v2 p = locations.get(i);
//
////                        @Override
////                        public Surface onTouch(Collidable body, ClosestRay hitPoint, short[] buttons, JoglPhysics space) {
////                            //if (buttons!=null && buttons.length > 0) {
////                                pp.packetLossRate.setValue(1f);
////                            //} else {
////                             //   pp.packetLossRate.setValue(0.05f); //back to normal
////                            //}
////                            return super.onTouch(body, hitPoint, buttons, space);
////                        }
//
//                        @Override
//                        public void commit(ConceptVis conceptVis, TermSpace space) {
//                            move(p.x, p.y, 0); //fix at its location
//                            super.commit(conceptVis, space);
//                        }
//                    };
//                };
//            }
//
//
//            @Override
//            protected long delay(InetSocketAddress from, InetSocketAddress to, int length) {
//                int a = from.getPort() - 10000; /* HACK */
//                int b = to.getPort() - 10000;
//
//                return Math.round(
//                        new v2().sub(locations.get(a), locations.get(b)).length() / (KmPerSec / 1000f /* ms */)
//                );
//            }
//
//            @Override
//            public void onTell(@Nullable UDPeer.UDProfile sender, UDPeer recv, UDPeer.Msg msg) {
//
//                @Nullable Concept from = n.conceptualize($.the(msg.port()));
//                @Nullable Concept to = n.conceptualize($.the(recv.port()));
//                float p = msgPri(msg);
//                from.termlinks().put(new PLink(to, p));
//
//
//                n.input(new Activate(from, 0.5f + p / 2f));
//                n.input(new Activate(to, 0.5f + p / 2f));
//
//                //System.out.println(from + " " + to.me + " " + m.edgeValueOrDefault(from, to.me, null));
//            }
//
//            private float msgPri(UDPeer.Msg m) {
//                return 0.001f;
//            }
//
//
//        };
//
//
//        for (UDPeer p : u.peer) {
//            float fps = (0.1f + 0.8f * n.random().nextFloat());
//            p.runFPS(fps);
//            p.them.setCapacity(Math.round(3 * fps));
//        }
//
//        n.onCycle(() -> {
//
//            n.forEachConcept(c -> {
//                c.termlinks().commit(x -> x.priMult(0.5f));
//            });
//
//            if (n.random().nextFloat() < 0.5f)
//                u.pingRandom(1);
//
//            if (n.random().nextFloat() < 0.5f)
//                u.tellSome(n.random().nextInt(u.peer.length), 2, 5);
//        });
//
//        n.startFPS(5);
//    }
//}
