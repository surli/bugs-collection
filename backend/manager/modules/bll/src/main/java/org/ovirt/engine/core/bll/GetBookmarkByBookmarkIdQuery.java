package org.ovirt.engine.core.bll;

import javax.inject.Inject;

import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.dao.BookmarkDao;

public class GetBookmarkByBookmarkIdQuery<P extends IdQueryParameters> extends QueriesCommandBase<P> {
    @Inject
    private BookmarkDao bookmarkDao;

    public GetBookmarkByBookmarkIdQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        getQueryReturnValue().setReturnValue(bookmarkDao.get(getParameters().getId()));
    }

}
