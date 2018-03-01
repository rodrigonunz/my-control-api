package com.mycontrol.api.audit;

import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.stereotype.Component;

@Component
public class AuditEventListener implements PostUpdateEventListener, PostInsertEventListener, PostDeleteEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8385953220529889418L;

	@Override
	public void onPostDelete(PostDeleteEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPostInsert(PostInsertEvent arg0) {
		// TODO Auto-generated method stub
		System.out.println(arg0.getEntity().toString());
	}

	@Override
	public void onPostUpdate(PostUpdateEvent arg0) {
		// TODO Auto-generated method stub
		System.out.println(arg0.getOldState().toString());
		System.out.println(arg0.getState().toString());
	}

	@Override
	public boolean requiresPostCommitHanding(EntityPersister arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
