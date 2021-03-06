package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.preproc.ImportDescriptor;
import com.google.common.collect.PeekingIterator;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor @Getter @EqualsAndHashCode(callSuper=false)
public class ImportDescriptorId extends AId<ImportDescriptor> {

	private final String importDescriptor;
	
	@Override
	public void collectComponents(List<Object> components) {
		components.add(importDescriptor);
	}
	
	public static enum Parser implements IId.Parser<ImportDescriptorId> {
		INSTANCE;
		
		@Override
		public ImportDescriptorId parse(PeekingIterator<String> parts) {
			return new ImportDescriptorId(parts.next());
		}
	}
}
