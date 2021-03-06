package nars.table;

import jcog.Skill;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.TaskConcept;
import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;


/** https://en.wikipedia.org/wiki/Compressed_sensing */
@Skill("Compressed_sensing")
public interface TemporalBeliefTable extends TaskTable {


    /**
     * range will be between 0 and 1
     */
    static float value(Task t, long start, long end, int dur) {
//        if (t.isDeleted())
//            return Float.NEGATIVE_INFINITY;

        //return ((1+t.conf()) * (1+t.priElseZero())) / (1f + Math.abs((start+end)/2 - t.mid())/((float)dur));
        //return t.conf() / (1f + t.distanceTo(start, end)/((float)dur));
        //return (float) (t.conf() / (1f + Math.log(1f+t.distanceTo(start, end)/((float)dur))));
        //return t.conf() / (1f + t.distanceTo(start, end)/dur);

//
//        //float fdur = dur;
//        //float range = t.range();

        float ee =
                t.confInteg()/dur;
                //t.eviInteg()/dur;

        long dt = t.minDistanceTo(start, end);

        if (dt == 0)
            return ee; //full integral
        else {
            //reduce the full integration by a factor relating to the distance (ex: isoceles triangle below)
            /*
            -----------+
                       |   .      .
                       |                 .     .
                       ^  - - - -  - - - - - - -
                      end                      ? <- time point being sampled
             */
            return (float) Param.evi(ee, dt, dur);
        }

                //t.evi() * (1/(1+t.minDistanceTo(start, end)/dur))
                //t.evi() * (1+Interval.intersectLength(start, end, t.start(), t.end()))

////                //t.conf(now, dur) *
////                //t.evi(now, dur) *
////                //* range == 0 ? 1f : (float) (1f + Math.sqrt(t.range()) / dur); ///(1+t.distanceTo(start, end)))); ///fdur

//        float fdur = dur;
//        return
//                //(1f + t.evi()) *
//                //(t.evi(start,end,dur))
//                (t.conf(start,end,dur))
//                * (float)Math.sqrt(1f + t.range()/fdur) //boost for duration
//                ;
//
//                //(1f + t.evi()) * //raw because time is considered below. this covers cases where the task eternalizes
//                //(1f / (1 + t.distanceTo(start, end)/fdur));
//
//                //(1f + t.conf()) * //raw because time is considered below. this covers cases where the task eternalizes
//                //t.evi(start,end,dur) *
//                //t.conf(now, dur) *
//                //t.evi(now, dur) *
//                /* ((float)Math.sqrt(1+t.range()/fdur)) */
//                 //1 / ((1 + t.distanceTo(start, end)/fdur));
    }

    /** finds or generates the strongest match to the specified parameters.
     * Task against is an optional argument which can be used to compare internal temporal dt structure for similarity */
    Task match(long start, long end, @Nullable Term against, NAR nar, Predicate<Task> filter);

    /** estimates the truth value for the provided time.
     * the eternal table's top value, if existent, contributes a 'background'
     * level in interpolation.
     * */
    Truth truth(long start, long end, EternalTable eternal, int dur);


    void setCapacity(int temporals);


    void update(SignalTask x, Runnable change);

    TemporalBeliefTable Empty = new TemporalBeliefTable() {

        @Override
        public boolean add(Task t, TaskConcept c, NAR n) {
            return false;
        }

        @Override
        public void update(SignalTask x, Runnable change) {

        }

        @Override
        public void setCapacity(int c) {

        }

        @Override
        public int capacity() {
            //throw new UnsupportedOperationException();
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean removeTask(Task x) {
            return false;
        }

        @Override
        public int size() {
            return 0;
        }


        @Override
        public Stream<Task> streamTasks() {
            return Stream.empty();
        }

           @Override
        public void forEachTask(Consumer<? super Task> x) {

        }

        @Override
        public void whileEach(Predicate<? super Task> each) {

        }

        @Override
        public void whileEach(long minT, long maxT, Predicate<? super Task> x) {

        }

        @Override
        public Task match(long start, long end, @Nullable Term against, NAR nar, Predicate<Task> filter) {
            return null;
        }

        @Override
        public Truth truth(long start, long end, EternalTable eternal, int dur) {
            return null;
        }

        @Override
        public void clear() {

        }
    };


    void whileEach(Predicate<? super Task> each);

    /** finds all temporally intersectnig tasks.  minT and maxT inclusive.  while the predicate remains true, it will continue scanning */
    default void whileEach(long minT, long maxT, Predicate<? super Task> each) {
        whileEach(x -> {
            if (!x.isDeleted() && x.end() >= minT && x.start() <= maxT)
                return each.test(x);
            else
                return true; //continue
        });
    }
}
