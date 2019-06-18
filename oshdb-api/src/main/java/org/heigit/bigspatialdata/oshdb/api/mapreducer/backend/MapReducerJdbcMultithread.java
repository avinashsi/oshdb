package org.heigit.bigspatialdata.oshdb.api.mapreducer.backend;

import com.google.common.collect.Streams;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.heigit.bigspatialdata.oshdb.api.db.OSHDBDatabase;
import org.heigit.bigspatialdata.oshdb.api.generic.function.SerializableBiFunction;
import org.heigit.bigspatialdata.oshdb.api.generic.function.SerializableBinaryOperator;
import org.heigit.bigspatialdata.oshdb.api.generic.function.SerializableFunction;
import org.heigit.bigspatialdata.oshdb.api.generic.function.SerializableSupplier;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.MapReducer;
import org.heigit.bigspatialdata.oshdb.api.mapreducer.backend.Kernels.CellProcessor;
import org.heigit.bigspatialdata.oshdb.api.object.OSHDBMapReducible;
import org.heigit.bigspatialdata.oshdb.api.object.OSMContribution;
import org.heigit.bigspatialdata.oshdb.api.object.OSMEntitySnapshot;
import org.heigit.bigspatialdata.oshdb.index.XYGridTree.CellIdRange;
import org.heigit.bigspatialdata.oshdb.osm.OSMType;
import org.heigit.bigspatialdata.oshdb.util.celliterator.CellIterator;
import org.heigit.bigspatialdata.oshdb.util.update.UpdateDbHelper;
import org.jetbrains.annotations.NotNull;
import org.json.simple.parser.ParseException;
import org.roaringbitmap.longlong.LongBitmapDataProvider;


public class MapReducerJdbcMultithread<X> extends MapReducerJdbc<X> {
  public MapReducerJdbcMultithread(OSHDBDatabase oshdb,
      Class<? extends OSHDBMapReducible> forClass) {
    super(oshdb, forClass);
  }

  // copy constructor
  private MapReducerJdbcMultithread(MapReducerJdbcMultithread obj) {
    super(obj);
  }

  @NotNull
  @Override
  protected MapReducer<X> copy() {
    return new MapReducerJdbcMultithread<X>(this);
  }

  @Override
  public boolean isCancelable() {
    return true;
  }

  private <S> S reduce(
      CellProcessor<S> processor,
      SerializableSupplier<S> identitySupplier,
      SerializableBinaryOperator<S> combiner
  ) throws ParseException, SQLException, IOException, ClassNotFoundException {
    this.executionStartTimeMillis = System.currentTimeMillis();

    CellIterator cellIterator = new CellIterator(
        this.tstamps.get(),
        this.bboxFilter, this.getPolyFilter(),
        this.getTagInterpreter(), this.getPreFilter(), this.getFilter(), false
    );
    //because streams are lazy we have to have two celliterators and cannot change the first one
    CellIterator updateIterator = new CellIterator(
        this.tstamps.get(),
        this.bboxFilter, this.getPolyFilter(),
        this.getTagInterpreter(), this.getPreFilter(), this.getFilter(), false
    );
    
    Map<OSMType, LongBitmapDataProvider> bitMapIndex = null;
    if (this.update != null) {
      bitMapIndex = UpdateDbHelper.getBitMap(
          this.update.getBitArrayDb()
      );
      cellIterator.excludeIDs(bitMapIndex);
    }

    final List<CellIdRange> cellIdRanges = new ArrayList<>();
    this.getCellIdRanges().forEach(cellIdRanges::add);

    Stream<S> oshdbStream = cellIdRanges.parallelStream()
        .filter(ignored -> this.isActive())
        .flatMap(this::getOshCellsStream)
        .filter(ignored -> this.isActive())
        .map(oshCell -> processor.apply(oshCell, cellIterator));

    Stream<S> updateStream = Stream.empty();
    if (this.update != null) {
      updateIterator.includeIDsOnly(bitMapIndex);
      updateStream = Streams.stream(this.getUpdates())
          .parallel()
          .filter(ignored -> this.isActive())
          .map(oshCell -> processor.apply(oshCell, updateIterator));;
    }
    
    return Streams.concat(oshdbStream, updateStream)
        .reduce(identitySupplier.get(), combiner);
  }

  private Stream<X> stream(
      CellProcessor<Collection<X>> processor
  ) throws ParseException, SQLException, IOException, ClassNotFoundException {
    this.executionStartTimeMillis = System.currentTimeMillis();

    CellIterator cellIterator = new CellIterator(
        this.tstamps.get(),
        this.bboxFilter, this.getPolyFilter(),
        this.getTagInterpreter(), this.getPreFilter(), this.getFilter(), false
    );
    //because streams are lazy we have to have two celliterators and cannot change the first one
    CellIterator updateIterator = new CellIterator(
        this.tstamps.get(),
        this.bboxFilter, this.getPolyFilter(),
        this.getTagInterpreter(), this.getPreFilter(), this.getFilter(), false
    );

    Map<OSMType, LongBitmapDataProvider> bitMapIndex = null;
    if (this.update != null) {
      bitMapIndex = UpdateDbHelper.getBitMap(
          this.update.getBitArrayDb()
      );
      cellIterator.excludeIDs(bitMapIndex);
    }

    final List<CellIdRange> cellIdRanges = new ArrayList<>();
    this.getCellIdRanges().forEach(cellIdRanges::add);

    Stream<X> oshdbStream = cellIdRanges.parallelStream()
        .filter(ignored -> this.isActive())
        .flatMap(this::getOshCellsStream)
        .filter(ignored -> this.isActive())
        .map(oshCell -> processor.apply(oshCell, cellIterator))
        .flatMap(Collection::stream);

    Stream<X> updateStream = Stream.empty();
    if (this.update != null) {
      updateIterator.includeIDsOnly(bitMapIndex);
      updateStream = Streams.stream(this.getUpdates())
          .parallel()
          .filter(ignored -> this.isActive())
          .map(oshCell -> processor.apply(oshCell, updateIterator))
          .flatMap(Collection::stream);
    }
    
    return Streams.concat(oshdbStream, updateStream);
  }

  // === map-reduce operations ===

  @Override
  protected <R, S> S mapReduceCellsOSMContribution(
      SerializableFunction<OSMContribution, R> mapper,
      SerializableSupplier<S> identitySupplier,
      SerializableBiFunction<S, R, S> accumulator,
      SerializableBinaryOperator<S> combiner
  ) throws Exception {
    return this.reduce(
        Kernels.getOSMContributionCellReducer(
            mapper,
            identitySupplier,
            accumulator,
            this
        ),
        identitySupplier,
        combiner
    );
  }

  @Override
  protected <R, S> S flatMapReduceCellsOSMContributionGroupedById(
      SerializableFunction<List<OSMContribution>, Iterable<R>> mapper,
      SerializableSupplier<S> identitySupplier,
      SerializableBiFunction<S, R, S> accumulator,
      SerializableBinaryOperator<S> combiner
  ) throws Exception {
    return this.reduce(
        Kernels.getOSMContributionGroupingCellReducer(
            mapper,
            identitySupplier,
            accumulator,
            this
        ),
        identitySupplier,
        combiner
    );
  }

  @Override
  protected <R, S> S mapReduceCellsOSMEntitySnapshot(
      SerializableFunction<OSMEntitySnapshot, R> mapper,
      SerializableSupplier<S> identitySupplier,
      SerializableBiFunction<S, R, S> accumulator,
      SerializableBinaryOperator<S> combiner
  ) throws Exception {
    return reduce(
        Kernels.getOSMEntitySnapshotCellReducer(
            mapper,
            identitySupplier,
            accumulator,
            this
        ),
        identitySupplier,
        combiner
    );
  }

  @Override
  protected <R, S> S flatMapReduceCellsOSMEntitySnapshotGroupedById(
      SerializableFunction<List<OSMEntitySnapshot>,Iterable<R>> mapper,
      SerializableSupplier<S> identitySupplier,
      SerializableBiFunction<S, R, S> accumulator,
      SerializableBinaryOperator<S> combiner
  ) throws Exception {
    return this.reduce(
        Kernels.getOSMEntitySnapshotGroupingCellReducer(
            mapper,
            identitySupplier,
            accumulator,
            this
        ),
        identitySupplier,
        combiner
    );
  }

  // === stream operations ===

  @Override
  protected Stream<X> mapStreamCellsOSMContribution(
      SerializableFunction<OSMContribution, X> mapper) throws Exception {
    return this.stream(Kernels.getOSMContributionCellStreamer(mapper, this));
  }

  @Override
  protected Stream<X> flatMapStreamCellsOSMContributionGroupedById(
      SerializableFunction<List<OSMContribution>, Iterable<X>> mapper) throws Exception {
    return this.stream(Kernels.getOSMContributionGroupingCellStreamer(mapper, this));
  }

  @Override
  protected Stream<X> mapStreamCellsOSMEntitySnapshot(
      SerializableFunction<OSMEntitySnapshot, X> mapper) throws Exception {
    return this.stream(Kernels.getOSMEntitySnapshotCellStreamer(mapper, this));
  }

  @Override
  protected Stream<X> flatMapStreamCellsOSMEntitySnapshotGroupedById(
      SerializableFunction<List<OSMEntitySnapshot>, Iterable<X>> mapper) throws Exception {
    return this.stream(Kernels.getOSMEntitySnapshotGroupingCellStreamer(mapper, this));
  }

}
