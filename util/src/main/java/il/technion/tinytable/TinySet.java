package il.technion.tinytable;


import il.technion.tinytable.bit.BitHelper;
import il.technion.tinytable.bit.BitwiseArray2;
import il.technion.tinytable.bit.BucketSizeExpert;
import il.technion.tinytable.hash.FingerPrintAux;
import il.technion.tinytable.hash.HashMakerWithIndexBit;

/** UNTESTED */
public class TinySet extends BitwiseArray2 {

	private final short[] A;
	// first level index.
	private final int nrItems;
	boolean bloomFilter;
	private final long[] L;
	private final HashMakerWithIndexBit hashFunc;
	private final BucketSizeExpert bucketMaster;
	public TinySet(int itemsize, int bucketCapacity,int nrBuckets,int maxAdditionalBits)
	{
		super(bucketCapacity*nrBuckets, itemsize+1,bucketCapacity);
		this.maxAdditionalSize = maxAdditionalBits;
		this.nrItems = 0;
		L = new long[nrBuckets];
		A = new short[nrBuckets];
		Items = new short[nrBuckets];
		hashFunc = new HashMakerWithIndexBit(itemsize+maxAdditionalSize, nrBuckets, 64,itemsize);
		this.bucketMaster = new BucketSizeExpert((itemsize+1)*bucketCapacity,itemsize+1,itemsize+maxAdditionalSize+1);
		//		temp = new long[200];
	}

	public void addItem(String item)
	{
		this.addItem(hashFunc.createHash(item));	
	}
	public void addItem(long item)
	{
		this.addItem(hashFunc.createHash(item));	
	}

	public void RemoveItem(String item)
	{

		if(!this.containItem(item))
		{
			System.out.println("Item not contained: "+ item);
			return;
		}
		FingerPrintAux fpaux = hashFunc.createHash(item);

		this.removeItem(fpaux.bucketId,fpaux.chainId,fpaux.fingerprint);	
	}


	private void removeItem(int bucketNumber, int chainNumber, long fingerPrint) {
		int actualSize = bucketBitSize-A[bucketNumber]*this.itemSize;
		int itemSize = this.bucketItemSize(bucketNumber,actualSize);
		int mod  = this.bucketMod(bucketNumber, itemSize);
		int bucketStart = this.bucketBitSize*bucketNumber + this.A[bucketNumber]*this.itemSize;
		int idx = chainStart(bucketStart,bucketNumber,chainNumber,itemSize,mod);
		//fingers can be two sizes, instead of constantly adjusting create two sizes and check them. 
		long sfingerPrint = BitHelper.fingerPrint(itemSize, fingerPrint);
		long lfingerPrint = BitHelper.fingerPrint(itemSize+1, fingerPrint);


		long otherFingerprint = this.Get(bucketNumber,idx,itemSize,mod);
		long fpTocomper = idx <mod ? lfingerPrint:sfingerPrint;


		while(!(FingerPrintAux.Equals(fpTocomper,otherFingerprint))) 
		{
			//			assertTrue(!FingerPrintAux.Equals(otherFingerprint,0l));
			if(FingerPrintAux.isLast(otherFingerprint))
			{
				System.err.println("Item not found!");
				return;
			}
			idx++;
			fpTocomper = idx <mod ? lfingerPrint:sfingerPrint;
			otherFingerprint = this.Get(bucketNumber,idx,itemSize,mod);
		}

		//		this.Put(bucketNumber, idx, BitHelper.markFingerPrintAsDeleted(otherFingerprint),itemSize,mod);

	}
	private boolean containItem(String item)
	{

		return this.containsItem(hashFunc.createHash(item));
	}
	public boolean containItem(long item)
	{

		return this.containsItem(hashFunc.createHash(item));
	}


	private int baseRank(int bucketNumber,int chainNumber)
	{
		long mask = ~((-1L)<<chainNumber);
		return Long.bitCount(L[bucketNumber]&mask); 
	}

	private int chainStart(int bucketStart, int bucketNumber, int chainNumber, int size, int mod)
	{
		return this.findChain(bucketStart, bucketNumber, 0, size, mod,baseRank(bucketNumber,chainNumber));

	}


	/**
	 * Adds a new fingerPrint to the following bucketNumber and chainNumber, the maximal size 
	 * of supported fingerprint is 64 bits, and it is assumed that the actual data sits on the LSB bits of
	 * long. 
	 * 
	 * According to our protocol, addition of a fingerprint may result in expending the bucket on account of neighboring buckets, 
	 * or down sizing the stored fingerprints to make room for the new one. 
	 * 
	 * In order to support deletions, deleted items are first logically deleted, and are fully 
	 * deleted only upon addition. 
	 * 
	 * @param bucketNumber
	 * @param chainNumber
	 * @param fingerPrint
	 */
	private void addItem(FingerPrintAux item)
	{
		int bucketSize = this.bucketBitSize - this.A[item.bucketId]*this.itemSize;
		// first, let's calculate the bucket size and mod, we do it now so we only do it once.
		int size = this.bucketItemSize(item.bucketId,bucketSize);
		int mod = this.bucketMod(item.bucketId, bucketSize);
		int newSize = this.nextItemSize(item.bucketId,bucketSize);
		int newMod = this.nextBucketMod(item.bucketId, bucketSize);
		// second, lets calculate the next item bucket size and mod.
		int bucketStart = this.bucketBitSize*item.bucketId + this.A[item.bucketId]*this.itemSize;
		int idxToAdd = chainStart(bucketStart,item.bucketId,item.chainId ,size,mod);

		if(!this.markChain(item.bucketId, item.chainId))
			item.fingerprint = FingerPrintAux.setLast(item.fingerprint);

		//find the next free bucket.
//		int nextBucket = this.findFreeBucket(item.bucketId);
//		// if we need to, we steal items from other buckets.
//		makeRoomForStolenItems(item.bucketId,nextBucket);
		// we may also need to make room for current buckets, and shrink the finger prints.
		item.fingerprint = allocateBucketItem(item.bucketId, item.fingerprint, idxToAdd,size,mod,newSize,newMod);
		//finally we put the item in the bucket, and perform a local bucket shifting as needed.
		this.putpush(item.bucketId, idxToAdd, item.fingerprint,newSize,newMod,item.chainId,false);
		//this.MarkChain(bucketNumber, chainNumber);
	}

	private long allocateBucketItem(int bucketNumber, long fingerPrint, int idx, int oldItemSize, int oldMod, int newItemSize, int newMod) {

		//first we check if a downsize of the items is required.
		if(oldItemSize!=newItemSize|| newMod!=oldMod)
		{
			//System.out.println(" Bucket: "+bucketNumber+ " Resizing items: "+ oldItemSize + " " + newItemSize + " "+ oldMod + " "+ newMod);
			resizeItems(bucketNumber,oldItemSize,newItemSize,oldMod,newMod,false);
		}
		// this is for debugging only now, we report a new item for the bucket.
		//		this.Items[bucketNumber]++;

		//we calculate the length of the new item size, and adjust the fingerprint to that size.
		newItemSize = idx<newMod?newItemSize+1:newItemSize;

		return BitHelper.fingerPrint(newItemSize, fingerPrint);
	}
	private void resizeItems(int bucketId,int oldSize, int newSize,int oldMod, int newMod,boolean IncrementAnchor) {


		//int modSize = newSize==this.maxAdditionalSize + this.itemSize? newSize: newSize+1;
		int i;
		int bucketStart = this.bucketBitSize*bucketId + this.A[bucketId]*this.itemSize;

		for( i=0; i<this.Items[bucketId];i++)
		{
			int nsize = newSize + i<newMod?1:0;
			int psize = oldSize + i<oldMod?1:0;
			if(nsize!=psize){
				long oldFp = this.Replace(bucketStart,bucketId,i,oldSize,oldMod, 0L);
				this.Put(bucketStart,bucketId,i, oldFp,newSize,newMod);
			}
		}
		if(IncrementAnchor){
			this.A[bucketId]++;
		}
	}




	@Override
    protected void Put(int bucketId, int idx, final long value) {
		int size = this.bucketItemSize(bucketId,bucketBitSize-A[bucketId]*this.itemSize);
		int mod = this.bucketMod(bucketId,size);
		this.Put(bucketId,idx, value,size,mod);
    }

	@Override
    protected long Get(int bucketID, int idx) {
		int size = this.bucketItemSize(bucketID,bucketBitSize-A[bucketID]*this.itemSize);
		int mod = bucketMod(bucketID,bucketBitSize-A[bucketID]*this.itemSize);
		return this.Get(bucketID,idx, size,mod);
	}


	private int bucketItemSize(int bucketNumber, int bucketSize)
	{
		//		int actualBucketSize = bucketBitSize-stolenItems*this.minSize;

		return this.bucketMaster.getSize(Items[bucketNumber],bucketSize);
	}

	private int nextItemSize(int bucketNumber, int bucketSize)
	{

		return this.bucketMaster.getSize(Items[bucketNumber]+1,bucketSize);
	}
	private int nextBucketMod(int bucketNumber, int actualBucketSize)
	{
		return this.bucketMaster.getMod(Items[bucketNumber]+1,actualBucketSize);
	}
	private int bucketMod(int bucketNumber, int actualBucketSize)
	{
		return this.bucketMaster.getMod(Items[bucketNumber],actualBucketSize);

	}

	private boolean containChain(int bucketNumber, int chainNumber)
	{
		//		long mask = ((1l)<<chainNumber);
		return (L[bucketNumber]&((1L)<<chainNumber)) != 0;
	}

	private boolean markChain(int bucketNumber, int chainNumber)
	{
		long mask = ((1L)<<chainNumber);
		boolean result = (L[bucketNumber]&mask) == mask;
		L[bucketNumber]|=mask;
		return result;
	}

	private boolean containsItem(FingerPrintAux item)
	{
		int bucketSize = this.bucketBitSize - this.A[item.bucketId]*this.itemSize;

		if(!containChain(item.bucketId,item.chainId))
			return false;
		int bucketStart = this.bucketBitSize*item.bucketId + this.A[item.bucketId]*this.itemSize;

		int itemSize = this.bucketItemSize(item.bucketId,bucketSize);
		int mod  = this.bucketMod(item.bucketId, bucketSize);
		//		int bucketStart = this.bucketBitSize*item.bucketId + this.A[item.bucketId]*this.itemSize;
		int idx = chainStart(bucketStart,item.bucketId,item.chainId,itemSize,mod);
		//fingers can be two sizes, instead of constantly adjusting create two sizes and check them.
		long sfingerPrint = item.fingerprint&((1<<itemSize)-1);

		//				BitHelper.adjustFingerPrint(itemSize, item.fingerprint);
		long lfingerPrint = item.fingerprint&((1<<(itemSize+1))-1);

		//				BitHelper.adjustFingerPrint(itemSize+1, item.fingerprint);


		long otherFingerprint = this.Get(bucketStart, idx,itemSize,mod);
		long fpTocomper = idx <mod ? lfingerPrint:sfingerPrint;

		//GIL: adding back the bug.
		while((fpTocomper^otherFingerprint)> 1L)
		{
			if((otherFingerprint& 1L) == 1L)
				return false;
			idx++;
			fpTocomper = idx <mod ? lfingerPrint:sfingerPrint;
			otherFingerprint = this.Get(bucketStart, idx,itemSize,mod);
		}
		return true;



	}




	/**
	 * Put a value at location idx, if the location is taken shift the items to
	 * be left until an open space is discovered.
	 *
	 * @param idx
	 *            - index to put in
	 * @param value
	 *            - value to put in
	 * @param mod
	 * 				- bucket mod, (in order to decode bucket)
	 * @param size
	 * 				- bucket item size. (in order to decode bucket)
	 * @param chainNumber
	 */
	private void putpush(int bucketId, int idx, final long value, int size, int mod, int chainNumber, boolean replaceDeleted) {

		int bucketStart = this.bucketBitSize*bucketId + this.A[bucketId]*this.itemSize;

		long itemToShift = value; 
		do{
			itemToShift = this.Replace(bucketStart,bucketId,idx,size,mod,itemToShift);
			idx++;
		}while(itemToShift>1);

		this.Items[bucketId]++;

	}
	public int getNrItems() {

		return this.nrItems;
	}












}
