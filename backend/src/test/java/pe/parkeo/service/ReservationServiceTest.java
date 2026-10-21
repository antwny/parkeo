package pe.parkeo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.parkeo.dto.request.CreateReservationRequest;
import pe.parkeo.dto.response.ReservationResponse;
import pe.parkeo.entity.*;
import pe.parkeo.enums.ReservationStatus;
import pe.parkeo.enums.SpaceStatus;
import pe.parkeo.exception.ConflictException;
import pe.parkeo.exception.UnauthorizedException;
import pe.parkeo.exception.ValidationException;
import pe.parkeo.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService Tests")
class ReservationServiceTest {

    @Mock ReservationRepository reservationRepository;
    @Mock ParkingSpaceRepository parkingSpaceRepository;
    @Mock ParkingLotRepository parkingLotRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock UserRepository userRepository;
    @Mock ParkingRatingRepository parkingRatingRepository;
    @Mock AvailabilityService availabilityService;

    @InjectMocks
    ReservationService reservationService;

    private User user;
    private User operator;
    private ParkingLot parkingLot;
    private ParkingSpace parkingSpace;
    private Vehicle vehicle;
    private VehicleType vehicleType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() {
        startTime = LocalDateTime.now().plusHours(1);
        endTime = LocalDateTime.now().plusHours(3);

        vehicleType = VehicleType.builder().id(1L).name("Auto").build();

        user = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .isActive(true)
                .build();

        operator = User.builder()
                .id(2L)
                .firstName("Carlos")
                .lastName("Ramos")
                .email("operador@parkeo.pe")
                .isActive(true)
                .build();

        parkingLot = ParkingLot.builder()
                .id(1L)
                .name("Parking Central")
                .address("Av. Principal 123")
                .latitude(-12.046374)
                .longitude(-77.042793)
                .totalSpaces(50)
                .availableSpaces(10)
                .operator(operator)
                .isActive(true)
                .isOpen(true)
                .build();

        parkingSpace = ParkingSpace.builder()
                .id(1L)
                .parkingLot(parkingLot)
                .spaceNumber("A-01")
                .floor("1")
                .section("A")
                .status(SpaceStatus.AVAILABLE)
                .vehicleType(vehicleType)
                .build();

        vehicle = Vehicle.builder()
                .id(1L)
                .user(user)
                .vehicleType(vehicleType)
                .licensePlate("ABC-123")
                .brand("Toyota")
                .model("Corolla")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("createReservation: debe crear reserva auto-confirmada exitosamente")
    void createReservation_shouldSucceed() {
        CreateReservationRequest request = CreateReservationRequest.builder()
                .parkingSpaceId(1L)
                .vehicleId(1L)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        Reservation savedReservation = Reservation.builder()
                .id(1L)
                .user(user)
                .parkingSpace(parkingSpace)
                .vehicle(vehicle)
                .parkingLot(parkingLot)
                .startTime(startTime)
                .endTime(endTime)
                .status(ReservationStatus.CONFIRMED)
                .totalAmount(new BigDecimal("20.00"))
                .currency("PEN")
                .confirmationCode("PKO-ABC12345")
                .build();

        when(vehicleRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reservationRepository.countOverlappingByVehicle(anyLong(), any(), any())).thenReturn(0L);
        when(parkingSpaceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(parkingSpace));
        when(reservationRepository.findOverlappingForUpdate(anyLong(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(availabilityService.calculatePrice(anyLong(), anyLong(), any(), any()))
                .thenReturn(new BigDecimal("20.00"));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);

        ReservationResponse response = reservationService.createReservation(request, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("20.00");
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    @DisplayName("createReservation: debe lanzar ConflictException cuando espacio ya está reservado")
    void createReservation_shouldThrowConflict_whenSpaceOverlaps() {
        CreateReservationRequest request = CreateReservationRequest.builder()
                .parkingSpaceId(1L)
                .vehicleId(1L)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        Reservation existingReservation = Reservation.builder()
                .id(99L)
                .parkingSpace(parkingSpace)
                .startTime(startTime.minusMinutes(30))
                .endTime(endTime.plusMinutes(30))
                .status(ReservationStatus.CONFIRMED)
                .build();

        when(vehicleRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reservationRepository.countOverlappingByVehicle(anyLong(), any(), any())).thenReturn(0L);
        when(parkingSpaceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(parkingSpace));
        when(reservationRepository.findOverlappingForUpdate(anyLong(), any(), any()))
                .thenReturn(List.of(existingReservation));

        assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("reservado");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("createReservation: debe lanzar ValidationException cuando endTime <= startTime")
    void createReservation_shouldThrowValidation_whenInvalidTimeRange() {
        CreateReservationRequest request = CreateReservationRequest.builder()
                .parkingSpaceId(1L)
                .vehicleId(1L)
                .startTime(startTime)
                .endTime(startTime.minusMinutes(30)) // end before start
                .build();

        assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("createReservation: debe lanzar ValidationException cuando espacio está en mantenimiento")
    void createReservation_shouldThrowValidation_whenSpaceInMaintenance() {
        parkingSpace.setStatus(SpaceStatus.MAINTENANCE);
        CreateReservationRequest request = CreateReservationRequest.builder()
                .parkingSpaceId(1L)
                .vehicleId(1L)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        when(vehicleRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reservationRepository.countOverlappingByVehicle(anyLong(), any(), any())).thenReturn(0L);
        when(parkingSpaceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(parkingSpace));

        assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("MAINTENANCE");
    }

    @Test
    @DisplayName("checkInReservation: debe registrar ingreso y poner espacio en OCCUPIED")
    void checkInReservation_shouldSucceed() {
        Reservation reservation = Reservation.builder()
                .id(10L)
                .user(user)
                .parkingSpace(parkingSpace)
                .parkingLot(parkingLot)
                .vehicle(vehicle)
                .startTime(LocalDateTime.now().plusMinutes(5)) // dentro de la ventana de check-in
                .endTime(endTime)
                .status(ReservationStatus.CONFIRMED)
                .confirmationCode("PKO-CHECKIN1")
                .totalAmount(new BigDecimal("15.00"))
                .currency("PEN")
                .build();

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(parkingSpaceRepository.countAvailableByParkingLotId(1L)).thenReturn(9L);

        ReservationResponse response = reservationService.checkInReservation(10L, 2L);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(parkingSpace.getStatus()).isEqualTo(SpaceStatus.OCCUPIED);
        assertThat(parkingLot.getAvailableSpaces()).isEqualTo(9);
    }

    @Test
    @DisplayName("checkInReservation: debe rechazar el ingreso cuando aún es muy temprano")
    void checkInReservation_shouldReject_whenTooEarly() {
        Reservation reservation = Reservation.builder()
                .id(10L)
                .user(user)
                .parkingSpace(parkingSpace)
                .parkingLot(parkingLot)
                .vehicle(vehicle)
                .startTime(LocalDateTime.now().plusHours(2)) // ventana abre en 90 min
                .endTime(endTime)
                .status(ReservationStatus.CONFIRMED)
                .build();

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.checkInReservation(10L, 2L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("muy temprano");
    }

    @Test
    @DisplayName("checkInReservation: debe rechazar cuando el operador no tiene acceso a la cochera")
    void checkInReservation_shouldThrowUnauthorized_whenOperatorMismatch() {
        Reservation reservation = Reservation.builder()
                .id(10L)
                .user(user)
                .parkingSpace(parkingSpace)
                .parkingLot(parkingLot)
                .vehicle(vehicle)
                .startTime(LocalDateTime.now().plusMinutes(5))
                .endTime(endTime)
                .status(ReservationStatus.PENDING)
                .build();

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));

        // Operator 99 is not assigned to parkingLot (operator id is 2)
        assertThatThrownBy(() -> reservationService.checkInReservation(10L, 99L))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("checkOutReservation: debe registrar salida, liberar espacio y fijar monto final")
    void checkOutReservation_shouldSucceed() {
        parkingSpace.setStatus(SpaceStatus.OCCUPIED);
        Reservation reservation = Reservation.builder()
                .id(10L)
                .user(user)
                .parkingSpace(parkingSpace)
                .parkingLot(parkingLot)
                .vehicle(vehicle)
                .startTime(LocalDateTime.now().minusHours(2))
                .endTime(LocalDateTime.now().plusMinutes(30))
                .status(ReservationStatus.ACTIVE)
                .confirmationCode("PKO-CHECKOUT1")
                .checkedInAt(LocalDateTime.now().minusHours(2))
                .totalAmount(new BigDecimal("15.00"))
                .currency("PEN")
                .build();

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(parkingSpaceRepository.countAvailableByParkingLotId(1L)).thenReturn(11L);
        when(availabilityService.calculatePrice(eq(1L), eq(1L), any(), any()))
                .thenReturn(new BigDecimal("18.00"));

        ReservationResponse response = reservationService.checkOutReservation(10L, 2L);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(parkingSpace.getStatus()).isEqualTo(SpaceStatus.AVAILABLE);
        assertThat(parkingLot.getAvailableSpaces()).isEqualTo(11);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("18.00");
    }

    @Test
    @DisplayName("cancelReservation: debe rechazar cancelar cuando la hora de inicio ya pasó")
    void cancelReservation_shouldReject_whenStartPassed() {
        Reservation reservation = Reservation.builder()
                .id(10L)
                .user(user)
                .parkingSpace(parkingSpace)
                .parkingLot(parkingLot)
                .vehicle(vehicle)
                .startTime(LocalDateTime.now().minusMinutes(30)) // ya pasó
                .endTime(LocalDateTime.now().plusHours(1))
                .status(ReservationStatus.CONFIRMED)
                .confirmationCode("PKO-CANCEL1")
                .build();

        when(reservationRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancelReservation(10L, 1L, "No voy a ir"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Ya no puedes cancelar");
    }

    @Test
    @DisplayName("rateReservation: debe rechazar calificar una reserva no completada")
    void rateReservation_shouldReject_whenNotCompleted() {
        Reservation reservation = Reservation.builder()
                .id(10L)
                .user(user)
                .parkingSpace(parkingSpace)
                .parkingLot(parkingLot)
                .vehicle(vehicle)
                .startTime(startTime)
                .endTime(endTime)
                .status(ReservationStatus.ACTIVE)
                .confirmationCode("PKO-RATE1")
                .build();

        when(reservationRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.rateReservation(10L, 1L, 5, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("completadas");
    }
}
