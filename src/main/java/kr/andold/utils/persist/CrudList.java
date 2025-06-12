package kr.andold.utils.persist;

import java.util.ArrayList;
import java.util.List;

import kr.andold.utils.Utility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrudList<Y> {
	@Builder.Default private List<Y> creates = new ArrayList<>();
	@Builder.Default private List<Y> duplicates = new ArrayList<>();
	@Builder.Default private List<Y> updates = new ArrayList<>();
	@Builder.Default private List<Y> removes = new ArrayList<>();

	public CrudList<Y> clear() {
		creates = new ArrayList<>();
		duplicates = new ArrayList<>();
		updates = new ArrayList<>();
		removes = new ArrayList<>();

		return this;
	}

	@Override
	public String toString() {
		return String.format("CrudList(creates: #%d, duplicates: #%d, updates: #%d, removes: #%d)", Utility.size(creates), Utility.size(duplicates), Utility.size(updates), Utility.size(removes));
	}

	public void add(CrudList<Y> crud) {
		this.creates.addAll(crud.getCreates());
		this.duplicates.addAll(crud.getDuplicates());
		this.updates.addAll(crud.getUpdates());
		this.removes.addAll(crud.getRemoves());
	}

	public boolean isEmpty() {
		return (creates == null || creates.isEmpty())
				&& (duplicates == null || duplicates.isEmpty())
				&& (updates == null || updates.isEmpty())
				&& (removes == null || removes.isEmpty())
				;
	}

}
