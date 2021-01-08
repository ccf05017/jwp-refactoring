package kitchenpos.application;

import kitchenpos.dao.OrderDao;
import kitchenpos.dao.OrderTableDao;
import kitchenpos.domain.OrderStatus;
import kitchenpos.domain.OrderTable;
import kitchenpos.domain.exceptions.orderTable.InvalidTryChangeEmptyException;
import kitchenpos.domain.exceptions.orderTable.InvalidTryChangeGuestsException;
import kitchenpos.domain.exceptions.orderTable.OrderTableEntityNotFoundException;
import kitchenpos.ui.dto.orderTable.OrderTableRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class OrderTableService {
    private final OrderDao orderDao;
    private final OrderTableDao orderTableDao;

    public OrderTableService(final OrderDao orderDao, final OrderTableDao orderTableDao) {
        this.orderDao = orderDao;
        this.orderTableDao = orderTableDao;
    }

    @Transactional
    public OrderTable create(final OrderTableRequest orderTableRequest) {
        OrderTable orderTable = new OrderTable();
        orderTable.setTableGroupId(null);
        orderTable.setEmpty(orderTableRequest.isEmpty());
        orderTable.setNumberOfGuests(orderTableRequest.getNumberOfGuests());

        return orderTableDao.save(orderTable);
    }

    public List<OrderTable> list() {
        return orderTableDao.findAll();
    }

    @Transactional
    public OrderTable changeEmpty(final Long orderTableId, final OrderTable orderTable) {
        final OrderTable savedOrderTable = orderTableDao.findById(orderTableId)
                .orElseThrow(() -> new OrderTableEntityNotFoundException("존재하지 않는 주문 테이블의 비움 상태를 바꿀 수 없습니다."));

        if (Objects.nonNull(savedOrderTable.getTableGroupId())) {
            throw new InvalidTryChangeEmptyException("단체 지정된 주문 테이블의 비움 상태를 바꿀 수 없습니다.");
        }

        if (orderDao.existsByOrderTableIdAndOrderStatusIn(
                orderTableId, Arrays.asList(OrderStatus.COOKING.name(), OrderStatus.MEAL.name()))) {
            throw new InvalidTryChangeEmptyException("조리중이거나 식사중인 주문 테이블의 비움 상태를 바꿀 수 없습니다.");
        }

        savedOrderTable.setEmpty(orderTable.isEmpty());

        return orderTableDao.save(savedOrderTable);
    }

    @Transactional
    public OrderTable changeNumberOfGuests(final Long orderTableId, final OrderTable orderTable) {
        final int numberOfGuests = orderTable.getNumberOfGuests();

        if (numberOfGuests < 0) {
            throw new IllegalArgumentException();
        }

        final OrderTable savedOrderTable = orderTableDao.findById(orderTableId)
                .orElseThrow(() -> new OrderTableEntityNotFoundException(
                        "존재하지 않는 주문 테이블의 방문한 손님 수를 바꿀 수 없습니다."));

        if (savedOrderTable.isEmpty()) {
            throw new InvalidTryChangeGuestsException("비어있는 주문 테이블의 방문한 손님 수를 바꿀 수 없습니다.");
        }

        savedOrderTable.setNumberOfGuests(numberOfGuests);

        return orderTableDao.save(savedOrderTable);
    }
}
