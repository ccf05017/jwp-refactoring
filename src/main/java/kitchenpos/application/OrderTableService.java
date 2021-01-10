package kitchenpos.application;

import kitchenpos.domain.orderTable.OrderTable;
import kitchenpos.domain.orderTable.OrderTableRepository;
import kitchenpos.domain.orderTable.exceptions.InvalidTryChangeGuestsException;
import kitchenpos.domain.orderTable.exceptions.OrderTableEntityNotFoundException;
import kitchenpos.infra.orderTable.OrderAdapter;
import kitchenpos.ui.dto.orderTable.ChangeEmptyRequest;
import kitchenpos.ui.dto.orderTable.ChangeNumberOfGuestsRequest;
import kitchenpos.ui.dto.orderTable.OrderTableRequest;
import kitchenpos.ui.dto.orderTable.OrderTableResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderTableService {
    private final OrderAdapter orderAdapter;
    private final OrderTableRepository orderTableRepository;

    public OrderTableService(final OrderAdapter orderAdapter, final OrderTableRepository orderTableRepository) {
        this.orderAdapter = orderAdapter;
        this.orderTableRepository = orderTableRepository;
    }

    @Transactional
    public OrderTableResponse create(final OrderTableRequest request) {
        OrderTable orderTable = new OrderTable(request.getNumberOfGuests(), request.isEmpty());
        OrderTable saved = orderTableRepository.save(orderTable);

        return OrderTableResponse.of(saved);
    }

    @Transactional(readOnly = true)
    public OrderTable findOrderTable(final Long id) {
        return orderTableRepository.findById(id)
                .orElseThrow(() -> new OrderTableEntityNotFoundException("해당 주문 테이블이 존재하지 않습니다."));
    }

    @Transactional(readOnly = true)
    public List<OrderTableResponse> list() {
        List<OrderTable> orderTables = orderTableRepository.findAll();

        return orderTables.stream()
                .map(OrderTableResponse::of)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderTableResponse changeEmpty(final Long orderTableId, final ChangeEmptyRequest changeEmptyRequest) {
        final OrderTable foundOrderTable = this.findOrderTable(orderTableId);

        orderAdapter.canChangeEmptyStatus(orderTableId);

        foundOrderTable.changeEmpty(changeEmptyRequest.isEmpty());

        return OrderTableResponse.of(foundOrderTable);
    }

    @Transactional
    public OrderTableResponse changeNumberOfGuests(
            final Long orderTableId, final ChangeNumberOfGuestsRequest changeNumberOfGuestsRequest
    ) {
        final int numberOfGuests = changeNumberOfGuestsRequest.getNumberOfGuests();

        final OrderTable foundOrderTable = this.findOrderTable(orderTableId);

        foundOrderTable.changeNumberOfGuests(numberOfGuests);

        return OrderTableResponse.of(foundOrderTable);
    }
}
