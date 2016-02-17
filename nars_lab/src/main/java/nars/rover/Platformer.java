package nars.rover;//package nars.rover;
//
//import ChainShape;
//import CircleShape;
//import EdgeShape;
//import PolygonShape;
//import MathUtils;
//import Vec2;
//import Body;
//import BodyDef;
//import BodyType;
//import FixtureDef;
//
///**
// * Created by me on 4/24/15.
// */
//
//public class Platformer extends PhysicsModel {
//    private static final long CHARACTER_TAG = 1231l;
//
//    private Body m_character;
//
////    @Override
////    public Long getTag(Body argBody) {
////        if (argBody == m_character) {
////            return CHARACTER_TAG;
////        }
////        return super.getTag(argBody);
////    }
////
////    @Override
////    public void processBody(Body argBody, Long argTag) {
////        if (argTag == CHARACTER_TAG) {
////            m_character = argBody;
////            return;
////        }
////        super.processBody(argBody, argTag);
////    }
////
////    @Override
////    public boolean isSaveLoadEnabled() {
////        return true;
////    }
//
//    @Override
//    public void initTest(boolean deserialized) {
//        if (deserialized) {
//            return;
//        }
//        // Ground body
//        {
//            BodyDef bd = new BodyDef();
//            Body ground = getWorld().createBody(bd);
//
//            EdgeShape shape = new EdgeShape();
//            shape.set(new Vec2(-20.0f, 0.0f), new Vec2(20.0f, 0.0f));
//            ground.createFixture(shape, 0.0f);
//        }
//
//        // Collinear edges
//        // This shows the problematic case where a box shape can hit
//        // an internal vertex.
//        {
//            BodyDef bd = new BodyDef();
//            Body ground = getWorld().createBody(bd);
//
//            EdgeShape shape = new EdgeShape();
//            shape.m_radius = 0.0f;
//            shape.set(new Vec2(-8.0f, 1.0f), new Vec2(-6.0f, 1.0f));
//            ground.createFixture(shape, 0.0f);
//            shape.set(new Vec2(-6.0f, 1.0f), new Vec2(-4.0f, 1.0f));
//            ground.createFixture(shape, 0.0f);
//            shape.set(new Vec2(-4.0f, 1.0f), new Vec2(-2.0f, 1.0f));
//            ground.createFixture(shape, 0.0f);
//        }
//
//        // Chain shape
//        {
//            BodyDef bd = new BodyDef();
//            bd.angle = 0.25f * MathUtils.PI;
//            Body ground = getWorld().createBody(bd);
//
//            Vec2[] vs = new Vec2[4];
//            vs[0] = new Vec2(5.0f, 7.0f);
//            vs[1] = new Vec2(6.0f, 8.0f);
//            vs[2] = new Vec2(7.0f, 8.0f);
//            vs[3] = new Vec2(8.0f, 7.0f);
//            ChainShape shape = new ChainShape();
//            shape.createChain(vs, 4);
//            ground.createFixture(shape, 0.0f);
//        }
//
//        // Square tiles. This shows that adjacency shapes may
//        // have non-smooth collision. There is no solution
//        // to this problem.
//        {
//            BodyDef bd = new BodyDef();
//            Body ground = getWorld().createBody(bd);
//
//            PolygonShape shape = new PolygonShape();
//            shape.setAsBox(1.0f, 1.0f, new Vec2(4.0f, 3.0f), 0.0f);
//            ground.createFixture(shape, 0.0f);
//            shape.setAsBox(1.0f, 1.0f, new Vec2(6.0f, 3.0f), 0.0f);
//            ground.createFixture(shape, 0.0f);
//            shape.setAsBox(1.0f, 1.0f, new Vec2(8.0f, 3.0f), 0.0f);
//            ground.createFixture(shape, 0.0f);
//        }
//
//        // Square made from an edge loop. Collision should be smooth.
//        {
//            BodyDef bd = new BodyDef();
//            Body ground = m_world.createBody(bd);
//
//            Vec2[] vs = new Vec2[4];
//            vs[0] = new Vec2(-1.0f, 3.0f);
//            vs[1] = new Vec2(1.0f, 3.0f);
//            vs[2] = new Vec2(1.0f, 5.0f);
//            vs[3] = new Vec2(-1.0f, 5.0f);
//            ChainShape shape = new ChainShape();
//            shape.createLoop(vs, 4);
//            ground.createFixture(shape, 0.0f);
//        }
//
//        // Edge loop. Collision should be smooth.
//        {
//            BodyDef bd = new BodyDef();
//            bd.position.set(-10.0f, 4.0f);
//            Body ground = getWorld().createBody(bd);
//
//            Vec2[] vs = new Vec2[10];
//            vs[0] = new Vec2(0.0f, 0.0f);
//            vs[1] = new Vec2(6.0f, 0.0f);
//            vs[2] = new Vec2(6.0f, 2.0f);
//            vs[3] = new Vec2(4.0f, 1.0f);
//            vs[4] = new Vec2(2.0f, 2.0f);
//            vs[5] = new Vec2(0.0f, 2.0f);
//            vs[6] = new Vec2(-2.0f, 2.0f);
//            vs[7] = new Vec2(-4.0f, 3.0f);
//            vs[8] = new Vec2(-6.0f, 2.0f);
//            vs[9] = new Vec2(-6.0f, 0.0f);
//            ChainShape shape = new ChainShape();
//            shape.createLoop(vs, 10);
//            ground.createFixture(shape, 0.0f);
//        }
//
//        // Square character 1
//        {
//            BodyDef bd = new BodyDef();
//            bd.position.set(-3.0f, 8.0f);
//            bd.type = BodyType.DYNAMIC;
//            bd.fixedRotation = true;
//            bd.allowSleep = false;
//
//            Body body = getWorld().createBody(bd);
//
//            PolygonShape shape = new PolygonShape();
//            shape.setAsBox(0.5f, 0.5f);
//
//            FixtureDef fd = new FixtureDef();
//            fd.shape = shape;
//            fd.density = 20.0f;
//            body.createFixture(fd);
//        }
//
//        // Square character 2
//        {
//            BodyDef bd = new BodyDef();
//            bd.position.set(-5.0f, 5.0f);
//            bd.type = BodyType.DYNAMIC;
//            bd.fixedRotation = true;
//            bd.allowSleep = false;
//
//            Body body = getWorld().createBody(bd);
//
//            PolygonShape shape = new PolygonShape();
//            shape.setAsBox(0.25f, 0.25f);
//
//            FixtureDef fd = new FixtureDef();
//            fd.shape = shape;
//            fd.density = 20.0f;
//            body.createFixture(fd);
//        }
//
//        // Hexagon character
//        {
//            BodyDef bd = new BodyDef();
//            bd.position.set(-5.0f, 8.0f);
//            bd.type = BodyType.DYNAMIC;
//            bd.fixedRotation = true;
//            bd.allowSleep = false;
//
//            Body body = getWorld().createBody(bd);
//
//            float angle = 0.0f;
//            float delta = MathUtils.PI / 3.0f;
//            Vec2 vertices[] = new Vec2[6];
//            for (int i = 0; i < 6; ++i) {
//                vertices[i] = new Vec2(0.5f * MathUtils.cos(angle), 0.5f * MathUtils.sin(angle));
//                angle += delta;
//            }
//
//            PolygonShape shape = new PolygonShape();
//            shape.set(vertices, 6);
//
//            FixtureDef fd = new FixtureDef();
//            fd.shape = shape;
//            fd.density = 20.0f;
//            body.createFixture(fd);
//        }
//
//        // Circle character
//        {
//            BodyDef bd = new BodyDef();
//            bd.position.set(3.0f, 5.0f);
//            bd.type = BodyType.DYNAMIC;
//            bd.fixedRotation = true;
//            bd.allowSleep = false;
//
//            Body body = getWorld().createBody(bd);
//
//            CircleShape shape = new CircleShape();
//            shape.m_radius = 0.5f;
//
//            FixtureDef fd = new FixtureDef();
//            fd.shape = shape;
//            fd.density = 20.0f;
//            body.createFixture(fd);
//        }
//
//        // Circle character
//        {
//            BodyDef bd = new BodyDef();
//            bd.position.set(-7.0f, 6.0f);
//            bd.type = BodyType.DYNAMIC;
//            bd.allowSleep = false;
//
//            m_character = getWorld().createBody(bd);
//
//            CircleShape shape = new CircleShape();
//            shape.m_radius = 0.25f;
//
//            FixtureDef fd = new FixtureDef();
//            fd.shape = shape;
//            fd.density = 20.0f;
//            fd.friction = 1;
//            m_character.createFixture(fd);
//        }
//    }
//
////    @Override
////    public void step(TestbedSettings settings) {
////        Vec2 v = m_character.getLinearVelocity();
////        v.x = -5f;
////
////        //super.step(settings);
////        addTextLine("This tests various character collision shapes");
////        addTextLine("Limitation: square and hexagon can snag on aligned boxes.");
////        addTextLine("Feature: edge chains have smooth collision inside and out.");
////    }
//
//    @Override
//    public String getTestName() {
//        return "Character Collision";
//    }
//}
//
