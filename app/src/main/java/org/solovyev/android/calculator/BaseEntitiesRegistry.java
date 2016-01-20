/*
 * Copyright 2013 serso aka se.solovyev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Contact details
 *
 * Email: se.solovyev@gmail.com
 * Site:  http://se.solovyev.org
 */

package org.solovyev.android.calculator;

import org.solovyev.android.calculator.model.EntityDao;
import org.solovyev.common.JBuilder;
import org.solovyev.common.math.MathEntity;
import org.solovyev.common.math.MathRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class BaseEntitiesRegistry<T extends MathEntity, P extends PersistedEntity> implements EntitiesRegistry<T> {

    @Nonnull
    private final MathRegistry<T> mathRegistry;

    @Nonnull
    private final String prefix;

    @Nonnull
    private final EntityDao<P> entityDao;

    protected BaseEntitiesRegistry(@Nonnull MathRegistry<T> mathRegistry,
                                   @Nonnull String prefix,
                                   @Nonnull EntityDao<P> entityDao) {
        this.mathRegistry = mathRegistry;
        this.prefix = prefix;
        this.entityDao = entityDao;
    }


    @Nonnull
    protected abstract Map<String, String> getSubstitutes();

    @Nullable
    @Override
    public String getDescription(@Nonnull String name) {
        final String stringName;

        final Map<String, String> substitutes = getSubstitutes();
        final String substitute = substitutes.get(name);
        if (substitute == null) {
            stringName = prefix + name;
        } else {
            stringName = prefix + substitute;
        }

        return entityDao.getDescription(stringName);
    }

    public synchronized void load() {
        final PersistedEntitiesContainer<P> persistenceContainer = entityDao.load();

        final List<P> notCreatedEntities = new ArrayList<P>();

        if (persistenceContainer != null) {
            for (P entity : persistenceContainer.getEntities()) {
                if (!contains(entity.getName())) {
                    try {
                        final JBuilder<? extends T> builder = createBuilder(entity);
                        add(builder);
                    } catch (RuntimeException e) {
                        Locator.getInstance().getErrorReporter().onException(e);
                        notCreatedEntities.add(entity);
                    }
                }
            }
        }

        try {
            if (!notCreatedEntities.isEmpty()) {
                final StringBuilder errorMessage = new StringBuilder(notCreatedEntities.size() * 100);
                for (P notCreatedEntity : notCreatedEntities) {
                    errorMessage.append(notCreatedEntity).append("\n\n");
                }

                Locator.getInstance().getCalculator().fireCalculatorEvent(CalculatorEventType.show_message_dialog, MessageDialogData.newInstance(CalculatorMessages.newErrorMessage(CalculatorMessages.msg_007, errorMessage.toString()), null));
            }
        } catch (RuntimeException e) {
            // just in case
            Locator.getInstance().getErrorReporter().onException(e);
        }
    }

    @Nonnull
    protected abstract JBuilder<? extends T> createBuilder(@Nonnull P entity);

    @Override
    public synchronized void save() {
        final PersistedEntitiesContainer<P> container = createPersistenceContainer();

        for (T entity : this.getEntities()) {
            if (!entity.isSystem()) {
                final P persistenceEntity = transform(entity);
                if (persistenceEntity != null) {
                    container.getEntities().add(persistenceEntity);
                }
            }
        }

        this.entityDao.save(container);
    }

    @Nullable
    protected abstract P transform(@Nonnull T entity);

    @Nonnull
    protected abstract PersistedEntitiesContainer<P> createPersistenceContainer();

    @Nonnull
    @Override
    public List<T> getEntities() {
        return mathRegistry.getEntities();
    }

    @Nonnull
    @Override
    public List<T> getSystemEntities() {
        return mathRegistry.getSystemEntities();
    }

    @Override
    public T add(@Nonnull JBuilder<? extends T> JBuilder) {
        return mathRegistry.add(JBuilder);
    }

    @Override
    public void remove(@Nonnull T var) {
        mathRegistry.remove(var);
    }

    @Nonnull
    @Override
    public List<String> getNames() {
        return mathRegistry.getNames();
    }

    @Override
    public boolean contains(@Nonnull String name) {
        return mathRegistry.contains(name);
    }

    @Override
    public T get(@Nonnull String name) {
        return mathRegistry.get(name);
    }

    @Override
    public T getById(@Nonnull Integer id) {
        return mathRegistry.getById(id);
    }
}