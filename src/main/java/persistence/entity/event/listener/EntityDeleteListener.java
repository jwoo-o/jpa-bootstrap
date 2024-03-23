package persistence.entity.event.listener;

import persistence.entity.event.EntityEvent;
import persistence.entity.event.action.ActionQueue;
import persistence.entity.event.action.DeleteEntityAction;
import persistence.sql.meta.MetaModel;

public class EntityDeleteListener extends AbstractEntityListener implements EntityListener {

    public EntityDeleteListener(MetaModel metaModel) {
        super(metaModel);
    }

    @Override
    public <T> T handleEvent(EntityEvent<T> event, ActionQueue actionQueue) {
        actionQueue.addAction(new DeleteEntityAction<>(metaModel.getEntityPersister(event.getEntityClass()),
            event.getEntity()), event.getActionType());
        return event.getEntity();
    }
}
