package eu.amidst.core.database.filereaders;

import eu.amidst.core.database.Attribute;
import eu.amidst.core.database.DataInstance;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Spliterators.spliterator;
import static java.util.stream.StreamSupport.stream;

public class DynamicDataInstanceSpliterator implements Spliterator<DataInstance> {

    private DataFileReader reader;
    private Iterator<DataRow> dataRowIterator;
    private Attribute attSequenceID;
    private Attribute attTimeID;
    private NextDynamicDataInstance nextDynamicDataInstance;

    private final Spliterator<DataRow> spliterator;
    //private final int batchSize;
    private final int characteristics;
    private long est;

    //public DynamicDataInstanceFixedBatchParallelSpliteratorWrapper(DataFileReader reader1, long est, int batchSize) {
    public DynamicDataInstanceSpliterator(DataFileReader reader1) {
        this.reader=reader1;
        dataRowIterator = this.reader.iterator();
        this.spliterator = this.reader.spliterator();

        final int c = spliterator.characteristics();
        this.characteristics = (c & SIZED) != 0 ? c | SUBSIZED : c;
        this.est = spliterator.estimateSize();
        //this.batchSize = batchSize;


        /**
         * We read the two first rows now, to create the first couple in next
         */
        DataRow present;
        DataRow past = new DataRowMissing();

        int timeID = 0;
        int sequenceID = 0;

        if (dataRowIterator.hasNext()) {
            present = this.dataRowIterator.next();
        }else {
            throw new UnsupportedOperationException("There are insufficient instances to learn a model.");
        }

        try {
            attSequenceID = this.reader.getAttributes().getAttributeByName("SEQUENCE_ID");
            sequenceID = (int)present.getValue(attSequenceID);
        }catch (UnsupportedOperationException e){
            attSequenceID = null;
        }
        try {
            attTimeID = this.reader.getAttributes().getAttributeByName("TIME_ID");
            timeID = (int)present.getValue(attTimeID);
        }catch (UnsupportedOperationException e){
            attTimeID = null;
        }

        nextDynamicDataInstance = new NextDynamicDataInstance(past, present, sequenceID, timeID);
    }


    //public DynamicDataInstanceFixedBatchParallelSpliteratorWrapper(Spliterator<DataRow> toWrap, int batchSize) {
    //    this(toWrap, toWrap.estimateSize(), batchSize);
    //}

    //public static Stream<DynamicDataInstance> toFixedBatchStream(Stream<DataRow> in, int batchSize) {
    //    return stream(new DynamicDataInstanceFixedBatchParallelSpliteratorWrapper(in.spliterator(), batchSize), true);
    //}

    public static Stream<DataInstance> toDynamicDataInstanceStream(DataFileReader reader) {
        return stream(new DynamicDataInstanceSpliterator(reader), false);
    }

    @Override public Spliterator<DataInstance> trySplit() {
        //this.reader.spliterator().trySplit()
        return null;
        /*
        final HoldingConsumer<DataRow> holder = new HoldingConsumer<>();
        if (!spliterator.tryAdvance(holder)) return null;
        final Object[] a = new Object[batchSize];
        int j = 0;
        do a[j] = holder.value; while (++j < batchSize && tryAdvance(holder));
        if (est != Long.MAX_VALUE) est -= j;
        return spliterator(a, 0, j, characteristics());
        */
    }

    @Override public boolean tryAdvance(Consumer<? super DataInstance> action) {

        if (!dataRowIterator.hasNext())
            return false;

        /* 0 = false, false, i.e., Not sequenceID nor TimeID are provided */
        /* 1 = true,  false, i.e., TimeID is provided */
        /* 2 = false, true,  i.e., SequenceID is provided */
        /* 3 = true,  true,  i.e., SequenceID is provided*/
        int option = ((attTimeID == null) ? 0 : 1) + 2 * ((attSequenceID == null) ? 0 : 1);

        switch (option) {

            /* Not sequenceID nor TimeID are provided*/
            case 0:
                action.accept(nextDynamicDataInstance.nextDataInstance_NoTimeID_NoSeq(dataRowIterator));
                return true;

             /* Only TimeID is provided*/
            case 1:
                action.accept(nextDynamicDataInstance.nextDataInstance_NoSeq(dataRowIterator, attTimeID));
                return true;

             /* Only SequenceID is provided*/
            case 2:
                action.accept(nextDynamicDataInstance.nextDataInstance_NoTimeID(dataRowIterator, attSequenceID));
                return true;

             /* SequenceID and TimeID are provided*/
            case 3:
                action.accept(nextDynamicDataInstance.nextDataInstance(dataRowIterator, attSequenceID, attTimeID));
                return true;
        }
        throw new IllegalArgumentException();
    }

    @Override public void forEachRemaining(Consumer<? super DataInstance> action) {
        while(this.dataRowIterator.hasNext()){
            this.tryAdvance(action);
        }
    }
    @Override public Comparator<DataInstance> getComparator() {
        if (hasCharacteristics(SORTED)) return null;
        throw new IllegalStateException();
    }

    @Override public long estimateSize() { return est; }
    @Override public int characteristics() { return characteristics; }

    static final class HoldingConsumer<T> implements Consumer<T> {
        Object value;
        @Override public void accept(T value) { this.value = value; }
    }
}
