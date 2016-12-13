package nars.nar.util;

import jcog.list.FasterList;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.bag.Bag;
import nars.budget.Budget;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nal.Deriver;
import nars.nal.Premise;
import nars.nal.meta.Derivation;
import nars.table.BeliefTable;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * derives matrix of: concept => (tasklink x termlink) => premises
 */
public class PremiseMatrix {

    private static final Logger logger = LoggerFactory.getLogger(PremiseMatrix.class);


    public static int run(@NotNull Concept c,
                          @NotNull NAR nar,
                          int tasklinks, int termlinks,
                          @NotNull Consumer<Task> target,
                          @NotNull Deriver deriver) {

        return run(c, nar, tasklinks, termlinks, target, deriver, c.tasklinks(), c.termlinks());
    }

    public static int run(@NotNull Concept c, @NotNull NAR nar, int tasklinks, int termlinks, @NotNull Consumer<Task> target, @NotNull Deriver deriver, @NotNull Bag<Task> tasklinkBag, @NotNull Bag<? extends Termed> termlinkBag) {
        int count = 0;

        try {

            c.commit();

            int tasklinksSampled = (int)Math.ceil(tasklinks * Param.BAG_OVERSAMPLING);

            FasterList<BLink<Task>> tasksBuffer = (FasterList)$.newArrayList(tasklinksSampled);

            tasklinkBag.sample(tasklinksSampled, tasksBuffer::addIfNotNull);

            int tasksBufferSize = tasksBuffer.size();
            if (tasksBufferSize > 0) {

                int termlinksSampled = (int)Math.ceil(termlinks * Param.BAG_OVERSAMPLING);

                FasterList<BLink<? extends Termed>> termsBuffer = (FasterList)$.newArrayList(termlinksSampled);
                termlinkBag.sample(termlinksSampled, termsBuffer::addIfNotNull);


                int termsBufferSize = termsBuffer.size();
                if (termsBufferSize > 0) {

                    //current termlink counter, as it cycles through what has been sampled, give it a random starting position
                    int jl = nar.random.nextInt(termsBufferSize);

                    //random starting position
                    int il = nar.random.nextInt(tasksBufferSize);

                    int countPerTasklink = 0;

                    for (int i = 0; i < tasksBufferSize && countPerTasklink < tasklinks; i++, il++) {

                        BLink<Task> taskLink = tasksBuffer.get( il % tasksBufferSize );

                        Task task =
                                taskLink.get();
                        /*match(taskLink.get(), nar);
                        if (task==null)
                            continue;*/

                        Budget taskLinkBudget = taskLink.clone(); //save a copy because in multithread, the original may be deleted in-between the sample result and now
                        if (taskLinkBudget == null)
                            continue;

                        long now = nar.time();

                        int countPerTermlink = 0;

                        for (int j = 0; j < termsBufferSize && countPerTermlink < termlinks; j++, jl++) {

                            Premise p = Premise.build(nar, c, now, task, termsBuffer.get( jl % termsBufferSize ).get().term() );

                            if (p != null) {

                                deriver.accept(new Derivation(nar, p, target));
                                countPerTermlink++;
                            }

                        }

                        countPerTasklink+= countPerTermlink > 0 ? 1 : 0;

                    }

                    count+=countPerTasklink;

                } else {
                    if (Param.DEBUG_EXTRA)
                        logger.warn("{} has zero termlinks", c);
                }

            } else {
                if (Param.DEBUG_EXTRA)
                    logger.warn("{} has zero tasklinks", c);
            }


        } catch (RuntimeException e) {

            //if (Param.DEBUG)
                e.printStackTrace();

            logger.error("run {}", e);
        }

        return count;
    }



    /** attempt to revise / match a better premise task */
    private static Task match(Task task, NAR nar) {

        if (!task.isInput() && task.isBeliefOrGoal()) {
            Concept c = task.concept(nar);

            long when = task.occurrence();

            if (c != null) {
                BeliefTable table = (BeliefTable) c.tableFor(task.punc());
                long now = nar.time();
                Task revised = table.match(when, now, task, false);
                if (revised!=null) {
                    if (task.isDeleted() || task.conf() < revised.conf()) {
                        task = revised;
                    }
                }

            }

        }

        if (task.isDeleted())
            return null;

        return task;
    }
}