package com.dmall.order.application;

import com.dmall.order.domain.IOrderRepository;
import com.dmall.order.domain.Order;
import com.dmall.order.domain.OrderEntity;
import com.dmall.order.domain.OrderEntityFactory;
import com.dmall.order.domain.OrderEvents;
import com.dmall.order.infrastructure.repository.OrderCancellationRepository;
import com.dmall.order.infrastructure.repository.OrderItemRepository;
import com.dmall.order.infrastructure.repository.OrderRepository;
import com.dmall.order.infrastructure.repository.PaymentRepository;
import com.dmall.order.infrastructure.repository.ShipmentRepository;
import com.dmall.order.interfaces.dto.CreateOrderRequest;
import com.dmall.order.interfaces.dto.CreateOrderResponse;
import com.dmall.order.interfaces.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional
// TODO: 19/10/2017 OrderService is a domain service, it implements IOrderRepository is reasonable.
public class OrderService implements IOrderRepository {

  @Autowired
  private OrderMapper orderMapper;

  @Autowired
  private OrderRepository repository;

  @Autowired
  private OrderItemRepository orderItemRepository;

  @Autowired
  private PaymentRepository paymentRepository;

  @Autowired
  private ShipmentRepository shipmentRepository;

  @Autowired
  private OrderEntityFactory orderEntityFactory;

  @Autowired
  private OrderCancellationRepository orderCancellationRepository;

  public CreateOrderResponse createOrder(CreateOrderRequest orderRequest) {
    final Order order = repository.save(orderMapper.fromApi(orderRequest));
    return orderMapper.toApi(order);
  }

  public void notifyPaid(Integer oid, String payment_id, String payment_time) {

    Message<OrderEvents> message = MessageBuilder
        .withPayload(OrderEvents.OrderPaid)
        .setHeader("payment_id", payment_id)
        .setHeader("payment_time", payment_time)
        .build();

    final OrderEntity order = orderEntityFactory.build(oid);
    order.sendEvent(message);
  }

  public void notifyInDelivery(Integer oid, String shipping_id, String shipping_time) {
    Message<OrderEvents> message = MessageBuilder
        .withPayload(OrderEvents.OrderShipped)
        .setHeader("shipping_id", shipping_id)
        .setHeader("shipping_time", shipping_time)
        .build();

    final OrderEntity order = orderEntityFactory.build(oid);
    order.sendEvent(message);
  }

  public void notifyReceived(Integer oid, Integer shipping_id, String received_time) {
    Message<OrderEvents> message = MessageBuilder
        .withPayload(OrderEvents.OrderReceived)
        .setHeader("shipping_id", shipping_id)
        .setHeader("received_time", received_time)
        .build();

    final OrderEntity order = orderEntityFactory.build(oid);

    order.sendEvent(message);
  }

  public void confirmOrder(Integer oid, String uid) {

    final OrderEntity order = orderEntityFactory.build(oid);
    order.sendEvent(OrderEvents.OrderConfirmed);
  }


  public void cancelOrder(Integer oid, Integer uid, String reason) {

    final OrderEntity order = orderEntityFactory.build(oid);


    if (!Objects.equals(uid, order.getOrder().getUid())) {
      throw new RuntimeException("The user is not match, cancellation is failed.");
    }

    Message<OrderEvents> message = MessageBuilder
        .withPayload(OrderEvents.OrderCancelled)
        .setHeader("reason", reason)
        .build();

    order.sendEvent(message);
  }

  public Order getOrderById(Integer oid) {
    Order order = repository.findOne(oid);

    if (Objects.nonNull(order)) {
      order.setPayment(paymentRepository.findByOid(oid));
      order.setShipment(shipmentRepository.findByOid(oid));
      order.setItems(orderItemRepository.findByOid(oid));
    }

    return order;
  }

  public Order save(Order order) {

    Order savedOrder = repository.save(order);

    if (Objects.nonNull(order.getItems())) {
      order.getItems().stream()
          .forEach(c -> c.setOid(savedOrder.getOid()));

      orderItemRepository.save(order.getItems());
    }

    if (Objects.nonNull(order.getPayment())) {
      paymentRepository.save(order.getPayment());
    }

    if (Objects.nonNull(order.getShipment())) {
      shipmentRepository.save(order.getShipment());
    }

    if (Objects.nonNull(order.getOrderCancellation())) {
      orderCancellationRepository.save(order.getOrderCancellation());
    }

    return savedOrder;
  }
}
