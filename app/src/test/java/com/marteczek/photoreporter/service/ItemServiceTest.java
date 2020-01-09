package com.marteczek.photoreporter.service;

import android.database.sqlite.SQLiteException;

import com.marteczek.photoreporter.application.MainThreadRunner;
import com.marteczek.photoreporter.database.ReportDatabaseHelper;
import com.marteczek.photoreporter.database.dao.ItemDao;
import com.marteczek.photoreporter.picturemanager.PictureManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class ItemServiceTest {

    @Mock
    ItemDao itemDao;

    @Mock
    PictureManager pictureManager;

    @Mock
    OnErrorListener onErrorListener;

    private ReportDatabaseHelper dbHelper= new ReportDatabaseHelperTestImpl();

    private MainThreadRunner mainThreadRunner = new MainThreadRunnerTestImpl();

    @Test
    public void updateItems_someHeadersChanged_databaseIsUpdated() {
        //given
        ItemService service = new ItemService(itemDao, pictureManager, dbHelper, mainThreadRunner);
        Map<Long, String> headerChanges = new HashMap<>();
        Set<Long> removedItems = new HashSet<>();
        List<Long> order = new ArrayList<>();
        headerChanges.put(3L, "value_3");
        headerChanges.put(5L, "value_5");
        //when
        service.updateItems(headerChanges, removedItems, order, null);
        //then
        verify(itemDao, times(1)).updateHeaderById(3L, "value_3");
        verify(itemDao, times(1)).updateHeaderById(5L, "value_5");
        verifyNoMoreInteractions(itemDao);
    }

    @Test
    public void updateItems_someItemsRemoved_databaseIsUpdated() {
        //given
        ItemService service = new ItemService(itemDao, pictureManager, dbHelper, mainThreadRunner);
        Map<Long, String> headerChanges = new HashMap<>();
        Set<Long> removedItems = new HashSet<>();
        List<Long> order = new ArrayList<>();
        removedItems.add(3L);
        removedItems.add(5L);
        //when
        service.updateItems(headerChanges, removedItems, order, null);
        //then
        verify(itemDao, times(1)).deleteById(3L);
        verify(itemDao, times(1)).deleteById(5L);
        verifyNoMoreInteractions(itemDao);
    }

    @Test
    public void updateItems_orderOfItemsIsChanged_databaseIsUpdated() {
        //given
        ItemService service = new ItemService(itemDao, pictureManager, dbHelper, mainThreadRunner);
        Map<Long, String> headerChanges = new HashMap<>();
        Set<Long> removedItems = new HashSet<>();
        List<Long> order = new ArrayList<>();
        order.add(3L);
        order.add(1L);
        order.add(2L);
        //when
        service.updateItems(headerChanges, removedItems, order, null);
        //then
        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> successionCaptor = ArgumentCaptor.forClass(Long.class);
        verify(itemDao, times(3)).updateSuccessionById(idCaptor.capture(), successionCaptor.capture());
        long firstNo = successionCaptor.getAllValues().get(0);
        verify(itemDao, times(1)).updateSuccessionById(3L, firstNo);
        verify(itemDao, times(1)).updateSuccessionById(1L, firstNo + 1);
        verify(itemDao, times(1)).updateSuccessionById(2L, firstNo + 2);
        verifyNoMoreInteractions(itemDao);
    }

    @Test
    public void updateItems_exceptionIsThrown_callbackFunctionIsCalled() {
        //given
        doThrow(new SQLiteException()).when(itemDao).deleteById(anyLong());
        ItemService service = new ItemService(itemDao, pictureManager, dbHelper, mainThreadRunner);
        Map<Long, String> headerChanges = new HashMap<>();
        Set<Long> removedItems = new HashSet<>();
        List<Long> order = new ArrayList<>();
        removedItems.add(3L);
        //when
        service.updateItems(headerChanges, removedItems, order, onErrorListener);
        //then
        verify(onErrorListener, times(1)).onError(any());
        verifyNoMoreInteractions(onErrorListener);
    }
}