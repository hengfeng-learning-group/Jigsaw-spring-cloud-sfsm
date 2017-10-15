package com.dmall.order.infrastructure.repository;

import com.dmall.order.domain.Order;
import com.dmall.order.domain.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class OrderRepositoryImpl implements OrderRepository {

  private OrderJpaRepository repository;

  private OrderItemJpaRepository orderItemJpaRepository;

  private PaymentJpaRepository paymentJpaRepository;

  @Autowired
  public OrderRepositoryImpl(OrderJpaRepository repository, OrderItemJpaRepository orderItemJpaRepository, PaymentJpaRepository paymentJpaRepository) {
    this.repository = repository;
    this.orderItemJpaRepository = orderItemJpaRepository;
    this.paymentJpaRepository = paymentJpaRepository;
  }

  @Override
  public Order getOrderById(Integer oid) {
    return repository.findOne(oid);
  }

  @Override
  public Order save(Order order) {

    Order savedOrder = repository.save(order);

    order.getItems().stream()
        .forEach(c -> c.setOid(savedOrder.getOid()));

    orderItemJpaRepository.save(order.getItems());

    return savedOrder;
  }

  @Override
  public void notifyPaid(Order order) {
    repository.save(order);
    paymentJpaRepository.save(order.getPayment());
  }
}
