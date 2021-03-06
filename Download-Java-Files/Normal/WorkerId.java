package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.google.common.collect.PeekingIterator;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor @Getter @EqualsAndHashCode(callSuper=false)
public class WorkerId extends AId<WorkerInformation> implements NamespacedId {

	private final DatasetId dataset;
	private final String worker;
	
	@Override
	public void collectComponents(List<Object> components) {
		dataset.collectComponents(components);
		components.add(worker);
	}
	
	public static enum Parser implements IId.Parser<WorkerId> {
		INSTANCE;
		
		@Override
		public WorkerId parse(PeekingIterator<String> parts) {
			return new WorkerId(DatasetId.Parser.INSTANCE.parse(parts), parts.next());
		}
	}
}
